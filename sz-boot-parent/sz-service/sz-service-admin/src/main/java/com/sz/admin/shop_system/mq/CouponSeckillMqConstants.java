package com.sz.admin.shop_system.mq;

/**
 * 优惠券抢购消息队列常量（所有命名均遵循跨引擎合法字符集 ^[a-zA-Z0-9_-]+$）
 */
public final class CouponSeckillMqConstants {

    private CouponSeckillMqConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String TOPIC = "coupon-seckill";

    public static final String TAG_GRAB = "grab";

    public static final String GROUP = "coupon-seckill-group";
}
