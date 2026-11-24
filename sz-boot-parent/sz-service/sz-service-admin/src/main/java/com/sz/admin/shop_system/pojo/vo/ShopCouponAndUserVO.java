package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "用户优惠券返回VO")
public class ShopCouponAndUserVO {
    @Schema(description = "用户优惠券ID（shop_product_coupon_user.id）")
    private Long id;

    @Schema(description = "优惠券模板ID（shop_product_coupon.id）")
    private Long cid;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "优惠券金额")
    private BigDecimal money;

    @Schema(description = "最低消费金额")
    private BigDecimal minMoney;

    @Schema(description = "领取结束时间（shop_product_coupon.coupon_end_time）")
    private LocalDateTime couponEndTime;

    @Schema(description = "可用开始时间")
    private LocalDateTime useStartTime;

    @Schema(description = "可用结束时间")
    private LocalDateTime useEndTime;

    @Schema(description = "用户ID")
    private Long uid;

    @Schema(description = "获取方式")
    private String type;

    @Schema(description = "状态：0未使用，1已使用，2已过期")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "使用时间")
    private LocalDateTime useTime;

    @Schema(description = "使用类型：1全场通用，2商品券，3品类券")
    private Integer useType;
}
