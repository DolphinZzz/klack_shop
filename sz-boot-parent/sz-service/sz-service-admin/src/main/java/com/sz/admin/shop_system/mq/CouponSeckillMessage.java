package com.sz.admin.shop_system.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优惠券抢购异步落库 MQ 消息载荷
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSeckillMessage {

    private String orderId;

    private Long couponId;

    private Long userId;

    private long requestTimeMillis;
}
