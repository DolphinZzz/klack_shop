package com.sz.admin.shop_system.mq;

import com.sz.admin.shop_system.mapper.ShopProductCouponMapper;
import com.sz.admin.shop_system.mapper.ShopProductCouponUserMapper;
import com.sz.admin.shop_system.pojo.po.ShopProductCoupon;
import com.sz.admin.shop_system.pojo.po.ShopProductCouponUser;
import com.sz.admin.shop_system.service.ShopProductCouponUserService;
import com.sz.mq.consumer.MqConsumerHandler;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqNonRetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 优惠券抢购异步落库消费者（零 MQ 引擎依赖，由 sz-common-mq 按 YAML 策略统一调度）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponSeckillOrderConsumer implements MqConsumerHandler<CouponSeckillMessage> {

    private static final int COUPON_TYPE_GRAB = 1;

    private final ShopProductCouponMapper shopProductCouponMapper;
    private final ShopProductCouponUserMapper shopProductCouponUserMapper;
    private final ShopProductCouponUserService shopProductCouponUserService;
    private final TransactionTemplate transactionTemplate;
    private final CouponSeckillRedisHelper couponSeckillRedisHelper;

    @Override
    public String topic() {
        return CouponSeckillMqConstants.TOPIC;
    }

    @Override
    public String tag() {
        return CouponSeckillMqConstants.TAG_GRAB;
    }

    @Override
    public String group() {
        return CouponSeckillMqConstants.GROUP;
    }

    @Override
    public Class<CouponSeckillMessage> payloadType() {
        return CouponSeckillMessage.class;
    }

    @Override
    public int concurrency() {
        return 1;
    }

    @Override
    public void onMessage(MqMessage<CouponSeckillMessage> message) {
        if (message == null || message.getPayload() == null) {
            throw new MqNonRetryableException("抢券消息载荷为空");
        }
        CouponSeckillMessage payload = message.getPayload();
        if (payload.getCouponId() == null || payload.getUserId() == null) {
            throw new MqNonRetryableException("抢券消息缺少必要字段: " + payload);
        }

        long startNanos = System.nanoTime();
        long queueWaitMs = payload.getRequestTimeMillis() > 0
                ? Math.max(0L, System.currentTimeMillis() - payload.getRequestTimeMillis())
                : 0L;
        log.info("[CouponSeckill] MQ消费者开始处理下单任务, messageId={}, orderId={}, couponId={}, userId={}, queueWaitMs={}",
                message.getMessageId(), payload.getOrderId(), payload.getCouponId(), payload.getUserId(), queueWaitMs);

        transactionTemplate.executeWithoutResult(status -> persistCouponOrderInTransaction(payload, startNanos));
    }

    @Override
    public void onConsumeFailed(MqMessage<CouponSeckillMessage> message, Throwable cause, boolean retryable) {
        if (message == null || message.getPayload() == null) {
            log.error("[CouponSeckill] 消费最终失败但消息体为空, cause={}", cause == null ? "unknown" : cause.getMessage(), cause);
            return;
        }
        CouponSeckillMessage payload = message.getPayload();
        if (!retryable) {
            log.error("[CouponSeckill] 确定性业务失败，触发Redis安全回滚并转入死信留痕, messageId={}, orderId={}, couponId={}, userId={}",
                    message.getMessageId(), payload.getOrderId(), payload.getCouponId(), payload.getUserId(), cause);
            couponSeckillRedisHelper.rollbackSeckill(payload.getCouponId(), payload.getUserId());
        } else {
            // 方式二：瞬时 DB 异常重试耗尽后保留 Redis 占坑（不回滚库存），转入死信队列等待 DB 恢复后人工重放补单
            log.error("[CouponSeckill] 瞬时异常重试耗尽，消息将进入死信队列待人工重放补单（保留Redis占坑不回滚）, messageId={}, orderId={}, couponId={}, userId={}",
                    message.getMessageId(), payload.getOrderId(), payload.getCouponId(), payload.getUserId(), cause);
        }
    }

    private void persistCouponOrderInTransaction(CouponSeckillMessage payload, long startNanos) {
        Long couponId = payload.getCouponId();
        Long userId = payload.getUserId();

        // 1. 数据库侧一人一单幂等兜底
        long receivedCount = shopProductCouponUserMapper.countByCidAndUid(couponId, userId);
        if (receivedCount > 0) {
            log.warn("[CouponSeckill] 异步落库跳过-数据库已存在领券记录, orderId={}, couponId={}, userId={}",
                    payload.getOrderId(), couponId, userId);
            return;
        }

        // 2. 校验优惠券有效性与时间窗口（确定性业务不满足则抛出 MqNonRetryableException，触发 Redis 回滚）
        ShopProductCoupon coupon = shopProductCouponMapper.selectOneById(couponId);
        if (coupon == null) {
            throw new MqNonRetryableException("优惠券不存在，couponId=" + couponId);
        }
        validateCouponCanGrab(coupon);

        // 3. 原子扣减数据库库存
        int affectedRows = shopProductCouponMapper.deductLastTotalAtomic(couponId);
        if (affectedRows != 1) {
            throw new MqNonRetryableException("数据库扣减优惠券库存失败（库存不足或已禁用），couponId=" + couponId);
        }

        // 4. 写入用户领券记录表
        ShopProductCouponUser couponUser = buildCouponUserRecord(coupon, userId);
        boolean saveSuccess = shopProductCouponUserService.save(couponUser);
        if (!saveSuccess) {
            throw new IllegalStateException("写入用户优惠券记录失败，couponId=" + couponId + ", userId=" + userId);
        }

        long dbCostMs = (System.nanoTime() - startNanos) / 1_000_000L;
        log.info("[CouponSeckill] MQ异步下单落库成功, orderId={}, couponUserId={}, couponId={}, userId={}, dbCostMs={}",
                payload.getOrderId(), couponUser.getId(), couponId, userId, dbCostMs);
    }

    private void validateCouponCanGrab(ShopProductCoupon coupon) {
        if (coupon.getStatus() != null && coupon.getStatus() != 0) {
            throw new MqNonRetryableException("该优惠券不可领取，couponId=" + coupon.getId());
        }
        if (coupon.getType() == null || coupon.getType() != COUPON_TYPE_GRAB) {
            throw new MqNonRetryableException("该优惠券不支持手动领取，couponId=" + coupon.getId());
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getCouponStartTime() != null && now.isBefore(coupon.getCouponStartTime())) {
            throw new MqNonRetryableException("该优惠券未到领取时间，couponId=" + coupon.getId());
        }
        if (coupon.getCouponEndTime() != null && now.isAfter(coupon.getCouponEndTime())) {
            throw new MqNonRetryableException("该优惠券已结束领取，couponId=" + coupon.getId());
        }
        if (coupon.getUseEndTime() != null && now.isAfter(coupon.getUseEndTime())) {
            throw new MqNonRetryableException("该优惠券已失效，couponId=" + coupon.getId());
        }
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
}
