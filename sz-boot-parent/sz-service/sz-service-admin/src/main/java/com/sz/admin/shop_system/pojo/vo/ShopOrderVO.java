package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * ShopOrder返回vo
 * </p>
 *
 * @author car
 * @since 2026-01-28
 */
@Data
@Schema(description = "ShopOrder返回vo")
public class ShopOrderVO {

    @Schema(description = "订单id")
    private Long id;

    @Schema(description = "订单号")
    private String orderId;

    @Schema(description = "用户姓名")
    private String realName;

    @Schema(description = "用户地址")
    private String userAddress;

    @Schema(description = "订单总价")
    private BigDecimal totalPrice;

    @Schema(description = "实际价格")
    private BigDecimal payPrice;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "支付方式")
    private String payType;

    @Schema(description = "支付状态 0未支付 1已支付 2支付失败")
    private Integer paid;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "订单类型(0普通 1秒杀 2砍价 3拼团)")
    private Integer orderType;

}

