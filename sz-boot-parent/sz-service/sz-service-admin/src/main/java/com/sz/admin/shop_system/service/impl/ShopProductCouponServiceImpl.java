package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopProductCouponMapper;
import com.sz.admin.shop_system.mapper.ShopProductCouponUserMapper;
import com.sz.admin.shop_system.pojo.dto.ShopProductCouponDTO;
import com.sz.admin.shop_system.pojo.po.ShopProductCoupon;
import com.sz.admin.shop_system.pojo.po.ShopProductCouponUser;
import com.sz.admin.shop_system.pojo.vo.ShopCouponAndProductsVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductCouponVO;
import com.sz.admin.shop_system.service.ShopProductCouponService;
import com.sz.admin.shop_system.service.ShopProductCouponUserService;
import com.sz.admin.shop_system.util.SerialNumberGenerator;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.datascope.SimpleDataScopeHelper;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.PageUtils;
import com.sz.security.util.StpUserUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>
 * 商品优惠券表 服务实现类
 * </p>
 *
 * @author car
 * @since 2026-01-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopProductCouponServiceImpl extends ServiceImpl<ShopProductCouponMapper, ShopProductCoupon> implements ShopProductCouponService {

    private static final String COUPON_STOCK_KEY_PREFIX = "coupon:vid:";
    private static final String COUPON_ORDER_KEY_PREFIX = "order:vid:";
    private static final int COUPON_TYPE_GRAB = 1;
    private static final long LUA_RESULT_SUCCESS = 0L;
    private static final long LUA_RESULT_NO_STOCK = 1L;
    private static final long LUA_RESULT_DUPLICATE = 2L;
    private static final int ORDER_QUEUE_CAPACITY = 100_000;

    private static final DefaultRedisScript<Long> COUPON_SECKILL_SCRIPT;

    static {
        COUPON_SECKILL_SCRIPT = new DefaultRedisScript<>();
        COUPON_SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/coupon_seckill.lua"));
        COUPON_SECKILL_SCRIPT.setResultType(Long.class);
    }

    private final ShopProductCouponUserMapper shopProductCouponUserMapper;
    private final ShopProductCouponUserService shopProductCouponUserService;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;

    /**
     * 秒杀优惠券异步下单阻塞队列
     */
    private final BlockingQueue<CouponSeckillOrderTask> orderTasks = new LinkedBlockingQueue<>(ORDER_QUEUE_CAPACITY);

    /**
     * 单线程异步消费阻塞队列，匀速写入数据库，避免高并发打满数据库连接池
     */
    private final ExecutorService couponOrderExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "coupon-seckill-order-worker");
        thread.setDaemon(true);
        return thread;
    });

    @PostConstruct
    private void startOrderWorker() {
        couponOrderExecutor.submit(this::consumeOrderTaskLoop);
    }

    @PreDestroy
    private void stopOrderWorker() {
        couponOrderExecutor.shutdownNow();
    }

    /**
     * 启动完成后将数据库中已有可抢购优惠券库存及已领取用户预热到 Redis（不覆盖已有实时库存）
     */
    @EventListener(ApplicationReadyEvent.class)
    public void preloadActiveCouponsToRedis() {
        try {
            List<ShopProductCoupon> activeCoupons = mapper.selectActiveGrabbableCoupons();
            if (activeCoupons == null || activeCoupons.isEmpty()) {
                return;
            }
            for (ShopProductCoupon coupon : activeCoupons) {
                if (coupon == null || coupon.getId() == null) {
                    continue;
                }
                int stock = coupon.getLastTotal() == null ? 0 : Math.max(coupon.getLastTotal(), 0);
                stringRedisTemplate.opsForValue().setIfAbsent(buildStockKey(coupon.getId()), String.valueOf(stock));

                List<Long> receivedUids = shopProductCouponUserMapper.selectUidsByCid(coupon.getId());
                if (receivedUids != null && !receivedUids.isEmpty()) {
                    String[] uidStrings = receivedUids.stream()
                            .filter(uid -> uid != null)
                            .map(String::valueOf)
                            .toArray(String[]::new);
                    if (uidStrings.length > 0) {
                        stringRedisTemplate.opsForSet().add(buildOrderKey(coupon.getId()), uidStrings);
                    }
                }
            }
            log.info("已预热 {} 张可抢购优惠券库存至 Redis", activeCoupons.size());
        } catch (Exception e) {
            log.warn("预热可抢购优惠券到 Redis 失败，将在创建/更新优惠券时同步: {}", e.getMessage());
        }
    }

    @Override
    public void create(ShopProductCouponDTO.Create dto) {
        validateCouponTimes(dto.getCouponStartTime(), dto.getCouponEndTime(), dto.getUseStartTime(), dto.getUseEndTime());
        validateMinMoney(dto.getMinMoney(), dto.getMoney());
        ShopProductCoupon shopProductCoupon = buildCouponForCreate(dto);
        save(shopProductCoupon);

        if (isGrabbableCoupon(shopProductCoupon) && shopProductCoupon.getId() != null) {
            int stock = shopProductCoupon.getLastTotal() == null ? 0 : Math.max(shopProductCoupon.getLastTotal(), 0);
            stringRedisTemplate.opsForValue().set(buildStockKey(shopProductCoupon.getId()), String.valueOf(stock));
        }
    }

    @Override
    public void update(ShopProductCouponDTO.Update dto) {
        validateCouponTimes(dto.getCouponStartTime(), dto.getCouponEndTime(), dto.getUseStartTime(), dto.getUseEndTime());
        validateMinMoney(dto.getMinMoney(), dto.getMoney());
        ShopProductCoupon existing = dto.getId() == null ? null : getById(dto.getId());
        CommonResponseEnum.INVALID_ID.assertTrue(existing == null);

        ShopProductCoupon shopProductCoupon = BeanCopyUtils.copy(dto, ShopProductCoupon.class);
        if (dto.getTotal() != null && existing.getTotal() != null && !dto.getTotal().equals(existing.getTotal())) {
            int baseLastTotal = existing.getLastTotal() == null ? existing.getTotal() : existing.getLastTotal();
            int nextLastTotal = Math.max(0, baseLastTotal + (dto.getTotal() - existing.getTotal()));
            shopProductCoupon.setLastTotal(nextLastTotal);
        }
        saveOrUpdate(shopProductCoupon);

        syncCouponStockToRedisAfterUpdate(existing, shopProductCoupon);
    }

    @Override
    public PageResult<ShopCouponAndProductsVO> page(ShopProductCouponDTO.ListQuery dto) {
        SimpleDataScopeHelper.start(ShopProductCoupon.class);
        PageUtils.toPage(dto);
        try {
            List<ShopCouponAndProductsVO> list = mapper.selectAdminPageList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            PageHelper.clearPage();
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    @Override
    public PageResult<ShopProductCouponVO> frontList(ShopProductCouponDTO.ListQuery dto) {
        SimpleDataScopeHelper.start(ShopProductCoupon.class);
        PageUtils.toPage(dto);
        try {
            LocalDateTime now = LocalDateTime.now();
            long userId = StpUserUtil.getLoginIdAsLong();
            List<ShopProductCouponVO> list = mapper.selectFrontList(dto, userId, now);
            enrichFrontListWithRedisState(list, userId);
            return PageUtils.getPageResult(list);
        } finally {
            PageHelper.clearPage();
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    /**
     * 基于 Redis + Lua 脚本 + 阻塞队列异步落库的抢优惠券实现
     *
     * @param couponId 优惠券 ID
     * @return 生成的秒杀订单号
     */
    @Override
    public String grabCoupon(Long couponId) {
        CommonResponseEnum.INVALID.message("优惠券ID不能为空").assertNull(couponId);
        long userId = StpUserUtil.getLoginIdAsLong();

        // 1. 执行 Lua 脚本，原子完成：库存判断 + 一人一单判断 + 预扣库存 + 记录已购用户 Set
        Long result = stringRedisTemplate.execute(
                COUPON_SECKILL_SCRIPT,
                Arrays.asList(buildStockKey(couponId), buildOrderKey(couponId)),
                String.valueOf(userId)
        );

        // 2. 判断秒杀资格标识（0：满足条件已下单、1：库存不足、2：该优惠券此用户已下过单）
        if (result == null || result == LUA_RESULT_NO_STOCK) {
            CommonResponseEnum.INVALID.message("优惠券库存不足，已抢完").assertTrue(true);
        }
        if (result == LUA_RESULT_DUPLICATE) {
            CommonResponseEnum.INVALID.message("该优惠券此用户已下过单，不能重复领取").assertTrue(true);
        }
        if (result != LUA_RESULT_SUCCESS) {
            CommonResponseEnum.INVALID.message("抢购优惠券失败，请稍后重试").assertTrue(true);
        }

        // 3. 有购买资格：生成订单号，将优惠券 ID、用户 ID、订单 ID 放入阻塞队列，等待异步线程落库
        String orderId = SerialNumberGenerator.generateBizOrderNo("CP");
        CouponSeckillOrderTask task = new CouponSeckillOrderTask(orderId, couponId, userId);
        boolean offered = orderTasks.offer(task);
        if (!offered) {
            rollbackRedisSeckill(couponId, userId);
            CommonResponseEnum.INVALID.message("当前抢购人数过多，请稍后重试").assertTrue(true);
        }

        // 4. 立即返回订单 ID，不阻塞等待数据库落库
        return orderId;
    }

    @Override
    public List<ShopProductCouponVO> list(ShopProductCouponDTO.ListQuery dto) {
        SimpleDataScopeHelper.start(ShopProductCoupon.class);
        try {
            return mapper.selectCouponList(dto);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    @Override
    public void remove(SelectIdsDTO dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getIds().isEmpty());
        removeByIds(dto.getIds());
        for (Serializable id : dto.getIds()) {
            if (id != null) {
                Long couponId = Long.valueOf(String.valueOf(id));
                stringRedisTemplate.delete(Arrays.asList(buildStockKey(couponId), buildOrderKey(couponId)));
            }
        }
    }

    @Override
    public ShopProductCouponVO detail(Object id) {
        ShopProductCoupon shopProductCoupon = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(shopProductCoupon);
        return BeanCopyUtils.copy(shopProductCoupon, ShopProductCouponVO.class);
    }

    // ==================== 异步队列消费与数据库落库 ====================

    private void consumeOrderTaskLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                CouponSeckillOrderTask task = orderTasks.take();
                handleCouponOrderTask(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("秒杀优惠券异步下单线程已停止");
                break;
            } catch (Exception e) {
                log.error("秒杀优惠券异步消费循环异常", e);
            }
        }
    }

    private void handleCouponOrderTask(CouponSeckillOrderTask task) {
        if (task == null) {
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> persistCouponOrderInTransaction(task));
        } catch (Exception e) {
            log.error("秒杀优惠券异步下单落库失败，回滚 Redis 预扣库存。orderId={}, couponId={}, userId={}",
                    task.getOrderId(), task.getCouponId(), task.getUserId(), e);
            rollbackRedisSeckill(task.getCouponId(), task.getUserId());
        }
    }

    private void persistCouponOrderInTransaction(CouponSeckillOrderTask task) {
        Long couponId = task.getCouponId();
        Long userId = task.getUserId();

        // 1. 数据库侧一人一单幂等兜底
        long receivedCount = shopProductCouponUserMapper.countByCidAndUid(couponId, userId);
        if (receivedCount > 0) {
            log.warn("异步下单检测到重复记录，跳过落库。orderId={}, couponId={}, userId={}",
                    task.getOrderId(), couponId, userId);
            return;
        }

        // 2. 校验优惠券有效性与时间窗口
        ShopProductCoupon coupon = getById(couponId);
        if (coupon == null) {
            throw new IllegalStateException("优惠券不存在，couponId=" + couponId);
        }
        validateCouponCanGrabInAsync(coupon);

        // 3. 原子扣减数据库库存
        int affectedRows = mapper.deductLastTotalAtomic(couponId);
        if (affectedRows != 1) {
            throw new IllegalStateException("数据库扣减优惠券库存失败，couponId=" + couponId);
        }

        // 4. 写入用户领券记录表
        ShopProductCouponUser couponUser = buildCouponUserRecord(coupon, userId);
        boolean saveSuccess = shopProductCouponUserService.save(couponUser);
        if (!saveSuccess) {
            throw new IllegalStateException("写入用户优惠券记录失败，couponId=" + couponId + ", userId=" + userId);
        }

        log.debug("秒杀优惠券异步下单落库成功。orderId={}, couponUserId={}, couponId={}, userId={}",
                task.getOrderId(), couponUser.getId(), couponId, userId);
    }

    private void rollbackRedisSeckill(Long couponId, Long userId) {
        try {
            stringRedisTemplate.opsForValue().increment(buildStockKey(couponId), 1L);
            stringRedisTemplate.opsForSet().remove(buildOrderKey(couponId), String.valueOf(userId));
        } catch (Exception ex) {
            log.error("回滚 Redis 秒杀优惠券数据失败，couponId={}, userId={}", couponId, userId, ex);
        }
    }

    // ==================== 辅助与校验方法 ====================

    private void enrichFrontListWithRedisState(List<ShopProductCouponVO> list, long userId) {
        if (list == null || list.isEmpty()) {
            return;
        }
        String userIdStr = String.valueOf(userId);
        for (ShopProductCouponVO vo : list) {
            if (vo == null || vo.getId() == null) {
                continue;
            }
            String redisStock = stringRedisTemplate.opsForValue().get(buildStockKey(vo.getId()));
            if (redisStock != null) {
                try {
                    vo.setLastTotal(Math.max(0, Integer.parseInt(redisStock)));
                } catch (NumberFormatException ignored) {
                    // 忽略非法缓存值，保留数据库值
                }
            }
            Boolean inOrderSet = stringRedisTemplate.opsForSet().isMember(buildOrderKey(vo.getId()), userIdStr);
            if (Boolean.TRUE.equals(inOrderSet)) {
                vo.setReceived(1);
            }
        }
    }

    private void syncCouponStockToRedisAfterUpdate(ShopProductCoupon existing, ShopProductCoupon updated) {
        Long couponId = updated.getId();
        if (couponId == null) {
            return;
        }
        Integer effectiveType = updated.getType() != null ? updated.getType() : existing.getType();
        Integer effectiveStatus = updated.getStatus() != null ? updated.getStatus() : existing.getStatus();
        if (Integer.valueOf(COUPON_TYPE_GRAB).equals(effectiveType) && (effectiveStatus == null || effectiveStatus == 0)) {
            Integer stock = updated.getLastTotal() != null
                    ? updated.getLastTotal()
                    : (existing.getLastTotal() != null ? existing.getLastTotal() : updated.getTotal());
            if (stock != null) {
                stringRedisTemplate.opsForValue().set(buildStockKey(couponId), String.valueOf(Math.max(stock, 0)));
            }
        } else {
            stringRedisTemplate.delete(buildStockKey(couponId));
        }
    }

    private boolean isGrabbableCoupon(ShopProductCoupon coupon) {
        return coupon != null
                && Integer.valueOf(COUPON_TYPE_GRAB).equals(coupon.getType())
                && (coupon.getStatus() == null || coupon.getStatus() == 0);
    }

    private String buildStockKey(Long couponId) {
        return COUPON_STOCK_KEY_PREFIX + couponId;
    }

    private String buildOrderKey(Long couponId) {
        return COUPON_ORDER_KEY_PREFIX + couponId;
    }

    private ShopProductCoupon buildCouponForCreate(ShopProductCouponDTO.Create dto) {
        ShopProductCoupon shopProductCoupon = BeanCopyUtils.copy(dto, ShopProductCoupon.class);
        shopProductCoupon.setLastTotal(dto.getTotal());
        return shopProductCoupon;
    }

    private ShopProductCouponUser buildCouponUserRecord(ShopProductCoupon coupon, long userId) {
        ShopProductCouponUser couponUser = new ShopProductCouponUser();
        couponUser.setCid(coupon.getId());
        couponUser.setUid(userId);
        couponUser.setType("get");
        couponUser.setStatus(0);
        couponUser.setUseType(resolveUseType(coupon.getCateKey()));
        return couponUser;
    }

    private void validateMinMoney(BigDecimal minMoney, BigDecimal money) {
        if (minMoney != null && money != null) {
            CommonResponseEnum.INVALID.message("最低消费额度不能大于优惠券面值")
                    .assertTrue(minMoney.compareTo(money) > 0);
        }
    }

    private void validateCouponTimes(LocalDateTime couponStartTime,
                                     LocalDateTime couponEndTime,
                                     LocalDateTime useStartTime,
                                     LocalDateTime useEndTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        if (couponStartTime != null) {
            CommonResponseEnum.INVALID.message("领取开始时间不能早于今天")
                    .assertTrue(couponStartTime.isBefore(todayStart));
        }
        if (couponEndTime != null) {
            CommonResponseEnum.INVALID.message("领取结束时间不能早于当前时间")
                    .assertTrue(couponEndTime.isBefore(now));
        }
        if (couponStartTime != null && couponEndTime != null) {
            CommonResponseEnum.INVALID.message("领取结束时间不能早于开始时间")
                    .assertTrue(couponEndTime.isBefore(couponStartTime));
        }

        if (useStartTime != null) {
            CommonResponseEnum.INVALID.message("可使用开始时间不能早于今天")
                    .assertTrue(useStartTime.isBefore(todayStart));
        }
        if (useEndTime != null) {
            CommonResponseEnum.INVALID.message("可使用结束时间不能早于今天")
                    .assertTrue(useEndTime.isBefore(todayStart));
        }
        if (useStartTime != null && useEndTime != null) {
            CommonResponseEnum.INVALID.message("可使用结束时间不能早于开始时间")
                    .assertTrue(useEndTime.isBefore(useStartTime));
        }
    }

    private void validateCouponCanGrabInAsync(ShopProductCoupon coupon) {
        if (coupon.getStatus() != null && coupon.getStatus() != 0) {
            throw new IllegalStateException("该优惠券不可领取，couponId=" + coupon.getId());
        }
        if (coupon.getType() == null || coupon.getType() != COUPON_TYPE_GRAB) {
            throw new IllegalStateException("该优惠券不支持手动领取，couponId=" + coupon.getId());
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getCouponStartTime() != null && now.isBefore(coupon.getCouponStartTime())) {
            throw new IllegalStateException("该优惠券未到领取时间，couponId=" + coupon.getId());
        }
        if (coupon.getCouponEndTime() != null && now.isAfter(coupon.getCouponEndTime())) {
            throw new IllegalStateException("该优惠券已结束领取，couponId=" + coupon.getId());
        }
        if (coupon.getUseEndTime() != null && now.isAfter(coupon.getUseEndTime())) {
            throw new IllegalStateException("该优惠券已失效，couponId=" + coupon.getId());
        }
    }

    private Integer resolveUseType(String cateKey) {
        if (cateKey == null || cateKey.isBlank()) {
            return 1;
        }
        List<String> cateTokens = Arrays.stream(cateKey.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        if (cateTokens.isEmpty() || cateTokens.contains("0")) {
            return 1;
        }
        return 2;
    }

    @Data
    @AllArgsConstructor
    private static class CouponSeckillOrderTask {
        private String orderId;
        private Long couponId;
        private Long userId;
    }
}
