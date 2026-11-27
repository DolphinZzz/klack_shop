package com.sz.admin.shop_system.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 优惠券秒杀 Redis Lua 预扣与幂等安全回滚助手
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponSeckillRedisHelper {

    public static final String COUPON_STOCK_KEY_PREFIX = "coupon:vid:";
    public static final String COUPON_ORDER_KEY_PREFIX = "order:vid:";
    public static final long LUA_RESULT_SUCCESS = 0L;
    public static final long LUA_RESULT_NO_STOCK = 1L;
    public static final long LUA_RESULT_DUPLICATE = 2L;

    private static final DefaultRedisScript<Long> COUPON_SECKILL_SCRIPT;
    private static final DefaultRedisScript<Long> COUPON_SECKILL_ROLLBACK_SCRIPT;

    static {
        COUPON_SECKILL_SCRIPT = new DefaultRedisScript<>();
        COUPON_SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/coupon_seckill.lua"));
        COUPON_SECKILL_SCRIPT.setResultType(Long.class);

        COUPON_SECKILL_ROLLBACK_SCRIPT = new DefaultRedisScript<>();
        COUPON_SECKILL_ROLLBACK_SCRIPT.setLocation(new ClassPathResource("lua/coupon_seckill_rollback.lua"));
        COUPON_SECKILL_ROLLBACK_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;

    public String buildStockKey(Long couponId) {
        return COUPON_STOCK_KEY_PREFIX + couponId;
    }

    public String buildOrderKey(Long couponId) {
        return COUPON_ORDER_KEY_PREFIX + couponId;
    }

    public Long tryPreDeduct(Long couponId, long userId) {
        String stockKey = buildStockKey(couponId);
        String orderKey = buildOrderKey(couponId);
        return stringRedisTemplate.execute(
                COUPON_SECKILL_SCRIPT,
                Arrays.asList(stockKey, orderKey),
                String.valueOf(userId)
        );
    }

    /**
     * 幂等安全回滚 Redis 预扣库存与已购集合：
     * <ul>
     *   <li>1：成功从已购集合移除用户并恢复库存 +1</li>
     *   <li>2：已移除用户，但库存 Key 已不存在（优惠券已禁用/删除），不加库存防止复活</li>
     *   <li>0：用户不在已购集合中（已回滚过），幂等跳过</li>
     * </ul>
     */
    public void rollbackSeckill(Long couponId, Long userId) {
        if (couponId == null || userId == null) {
            return;
        }
        String stockKey = buildStockKey(couponId);
        String orderKey = buildOrderKey(couponId);
        try {
            Long outcome = stringRedisTemplate.execute(
                    COUPON_SECKILL_ROLLBACK_SCRIPT,
                    Arrays.asList(stockKey, orderKey),
                    String.valueOf(userId)
            );
            log.warn("[CouponSeckill] Redis预扣库存安全回滚执行完成, couponId={}, userId={}, stockKey={}, orderKey={}, outcome={}",
                    couponId, userId, stockKey, orderKey, outcome);
        } catch (Exception ex) {
            log.error("[CouponSeckill] Redis预扣库存安全回滚失败，请人工核对一致性, couponId={}, userId={}, stockKey={}, orderKey={}",
                    couponId, userId, stockKey, orderKey, ex);
        }
    }
}
