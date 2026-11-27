package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopProductCouponMapper;
import com.sz.admin.shop_system.mapper.ShopProductCouponUserMapper;
import com.sz.admin.shop_system.mq.CouponSeckillMessage;
import com.sz.admin.shop_system.mq.CouponSeckillMqConstants;
import com.sz.admin.shop_system.mq.CouponSeckillRedisHelper;
import com.sz.admin.shop_system.pojo.dto.ShopProductCouponDTO;
import com.sz.admin.shop_system.pojo.po.ShopProductCoupon;
import com.sz.admin.shop_system.pojo.vo.ShopCouponAndProductsVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductCouponVO;
import com.sz.admin.shop_system.service.ShopProductCouponService;
import com.sz.admin.shop_system.util.SerialNumberGenerator;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.datascope.SimpleDataScopeHelper;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.PageUtils;
import com.sz.mq.strategy.MqTemplate;
import com.sz.security.util.StpUserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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

    private static final int COUPON_TYPE_GRAB = 1;

    private final ShopProductCouponUserMapper shopProductCouponUserMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MqTemplate mqTemplate;
    private final CouponSeckillRedisHelper couponSeckillRedisHelper;

    /**
     * 启动完成后将数据库中已有可抢购优惠券库存及已领取用户预热到 Redis（不覆盖已有实时库存）
     */
    @EventListener(ApplicationReadyEvent.class)
    public void preloadActiveCouponsToRedis() {
        try {
            List<ShopProductCoupon> activeCoupons = mapper.selectActiveGrabbableCoupons();
            if (activeCoupons == null || activeCoupons.isEmpty()) {
                log.info("[CouponSeckill] 启动预热完成，当前无可抢购优惠券");
                return;
            }
            for (ShopProductCoupon coupon : activeCoupons) {
                if (coupon == null || coupon.getId() == null) {
                    continue;
                }
                int stock = coupon.getLastTotal() == null ? 0 : Math.max(coupon.getLastTotal(), 0);
                String stockKey = buildStockKey(coupon.getId());
                Boolean initialized = stringRedisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(stock));

                int preloadedUserCount = 0;
                List<Long> receivedUids = shopProductCouponUserMapper.selectUidsByCid(coupon.getId());
                if (receivedUids != null && !receivedUids.isEmpty()) {
                    String[] uidStrings = receivedUids.stream()
                            .filter(uid -> uid != null)
                            .map(String::valueOf)
                            .toArray(String[]::new);
                    if (uidStrings.length > 0) {
                        stringRedisTemplate.opsForSet().add(buildOrderKey(coupon.getId()), uidStrings);
                        preloadedUserCount = uidStrings.length;
                    }
                }
                log.info("[CouponSeckill] 预热优惠券缓存, couponId={}, stockKey={}, dbStock={}, initialized={}, preloadedUsers={}",
                        coupon.getId(), stockKey, stock, Boolean.TRUE.equals(initialized), preloadedUserCount);
            }
            log.info("[CouponSeckill] 已完成 {} 张可抢购优惠券库存预热至 Redis", activeCoupons.size());
        } catch (Exception e) {
            log.warn("[CouponSeckill] 预热可抢购优惠券到 Redis 失败，将在创建/更新优惠券时同步, reason={}", e.getMessage(), e);
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
            String stockKey = buildStockKey(shopProductCoupon.getId());
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
            log.info("[CouponSeckill] 创建可抢购优惠券并同步库存至 Redis, couponId={}, stockKey={}, stock={}",
                    shopProductCoupon.getId(), stockKey, stock);
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
     * 基于 Redis + Lua 脚本 + 消息队列（MQ）异步落库的抢优惠券实现
     *
     * @param couponId 优惠券 ID
     * @return 生成的秒杀订单号
     */
    @Override
    public String grabCoupon(Long couponId) {
        CommonResponseEnum.INVALID.message("优惠券ID不能为空").assertNull(couponId);
        long userId = StpUserUtil.getLoginIdAsLong();
        String stockKey = couponSeckillRedisHelper.buildStockKey(couponId);
        String orderKey = couponSeckillRedisHelper.buildOrderKey(couponId);
        long startNanos = System.nanoTime();

        log.info("[CouponSeckill] 开始执行抢券资格校验, couponId={}, userId={}, stockKey={}, orderKey={}",
                couponId, userId, stockKey, orderKey);

        // 1. 执行 Lua 脚本，原子完成：库存判断 + 一人一单判断 + 预扣库存 + 记录已购用户 Set
        Long result = couponSeckillRedisHelper.tryPreDeduct(couponId, userId);
        long luaCostMs = (System.nanoTime() - startNanos) / 1_000_000L;

        // 2. 判断秒杀资格标识（0：满足条件已下单、1：库存不足、2：该优惠券此用户已下过单）
        if (result == null || result == CouponSeckillRedisHelper.LUA_RESULT_NO_STOCK) {
            log.warn("[CouponSeckill] 抢券拦截-库存不足, couponId={}, userId={}, luaResult={}, luaCostMs={}",
                    couponId, userId, result, luaCostMs);
            CommonResponseEnum.INVALID.message("优惠券库存不足，已抢完").assertTrue(true);
        }
        if (result == CouponSeckillRedisHelper.LUA_RESULT_DUPLICATE) {
            log.warn("[CouponSeckill] 抢券拦截-一人一单重复下单, couponId={}, userId={}, luaResult={}, luaCostMs={}",
                    couponId, userId, result, luaCostMs);
            CommonResponseEnum.INVALID.message("该优惠券此用户已下过单，不能重复领取").assertTrue(true);
        }
        if (result != CouponSeckillRedisHelper.LUA_RESULT_SUCCESS) {
            log.error("[CouponSeckill] 抢券失败-Lua脚本返回异常状态, couponId={}, userId={}, luaResult={}, luaCostMs={}",
                    couponId, userId, result, luaCostMs);
            CommonResponseEnum.INVALID.message("抢购优惠券失败，请稍后重试").assertTrue(true);
        }

        log.info("[CouponSeckill] Lua脚本校验通过并完成Redis预扣库存, couponId={}, userId={}, luaCostMs={}",
                couponId, userId, luaCostMs);

        // 3. 有购买资格：生成订单号，通过策略模式路由至当前启用的 MQ（RabbitMQ / RocketMQ / Kafka）异步落库
        String orderId = SerialNumberGenerator.generateBizOrderNo("CP");
        CouponSeckillMessage message = CouponSeckillMessage.builder()
                .orderId(orderId)
                .couponId(couponId)
                .userId(userId)
                .requestTimeMillis(System.currentTimeMillis())
                .build();
        try {
            mqTemplate.send(
                    CouponSeckillMqConstants.TOPIC,
                    CouponSeckillMqConstants.TAG_GRAB,
                    orderId,
                    message
            );
        } catch (Exception e) {
            log.error("[CouponSeckill] 发送抢券MQ消息失败，触发Redis回滚, orderId={}, couponId={}, userId={}, mqType={}",
                    orderId, couponId, userId, mqTemplate.currentType(), e);
            couponSeckillRedisHelper.rollbackSeckill(couponId, userId);
            CommonResponseEnum.INVALID.message("抢购请求提交失败，请稍后重试").assertTrue(true);
        }

        long totalCostMs = (System.nanoTime() - startNanos) / 1_000_000L;
        log.info("[CouponSeckill] 抢券任务已投递MQ并返回订单号, mqType={}, orderId={}, couponId={}, userId={}, totalCostMs={}",
                mqTemplate.currentType(), orderId, couponId, userId, totalCostMs);

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
                String stockKey = buildStockKey(couponId);
                String orderKey = buildOrderKey(couponId);
                stringRedisTemplate.delete(Arrays.asList(stockKey, orderKey));
                log.info("[CouponSeckill] 删除优惠券并清理Redis缓存, couponId={}, stockKey={}, orderKey={}",
                        couponId, stockKey, orderKey);
            }
        }
    }

    @Override
    public ShopProductCouponVO detail(Object id) {
        ShopProductCoupon shopProductCoupon = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(shopProductCoupon);
        return BeanCopyUtils.copy(shopProductCoupon, ShopProductCouponVO.class);
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
        String stockKey = buildStockKey(couponId);
        Integer effectiveType = updated.getType() != null ? updated.getType() : existing.getType();
        Integer effectiveStatus = updated.getStatus() != null ? updated.getStatus() : existing.getStatus();
        if (Integer.valueOf(COUPON_TYPE_GRAB).equals(effectiveType) && (effectiveStatus == null || effectiveStatus == 0)) {
            Integer stock = updated.getLastTotal() != null
                    ? updated.getLastTotal()
                    : (existing.getLastTotal() != null ? existing.getLastTotal() : updated.getTotal());
            if (stock != null) {
                int normalizedStock = Math.max(stock, 0);
                stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(normalizedStock));
                log.info("[CouponSeckill] 更新优惠券并同步Redis库存, couponId={}, stockKey={}, stock={}",
                        couponId, stockKey, normalizedStock);
            }
        } else {
            stringRedisTemplate.delete(stockKey);
            log.info("[CouponSeckill] 优惠券变更为非抢购/不可用状态，清理Redis库存Key, couponId={}, stockKey={}",
                    couponId, stockKey);
        }
    }

    private boolean isGrabbableCoupon(ShopProductCoupon coupon) {
        return coupon != null
                && Integer.valueOf(COUPON_TYPE_GRAB).equals(coupon.getType())
                && (coupon.getStatus() == null || coupon.getStatus() == 0);
    }

    private String buildStockKey(Long couponId) {
        return couponSeckillRedisHelper.buildStockKey(couponId);
    }

    private String buildOrderKey(Long couponId) {
        return couponSeckillRedisHelper.buildOrderKey(couponId);
    }

    private ShopProductCoupon buildCouponForCreate(ShopProductCouponDTO.Create dto) {
        ShopProductCoupon shopProductCoupon = BeanCopyUtils.copy(dto, ShopProductCoupon.class);
        shopProductCoupon.setLastTotal(dto.getTotal());
        return shopProductCoupon;
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
}
