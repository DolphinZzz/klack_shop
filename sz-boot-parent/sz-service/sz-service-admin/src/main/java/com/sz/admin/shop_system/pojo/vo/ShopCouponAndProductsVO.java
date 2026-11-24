package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author car
 * @create 2026/1/30
 */
@Data
@Schema(description = "后台管理-优惠券列表返回VO")
public class ShopCouponAndProductsVO extends  ShopProductCouponVO {
    @Schema(description = "用户优惠券id（shop_product_coupon_user.id）")
    private Long couponUserId;

    @Schema(description = "商品名")
    private String productNames;

    @Schema(description = "优惠券开始领取天数")
    private String couponStartDays;

    @Schema(description = "优惠券结束领取天数")
    private String couponEndDays;

    @Schema(description = "优惠券开始使用天数")
    private String useStartDays;

    @Schema(description = "优惠券结束使用天数")
    private String useEndDays;

    @Schema(description = "可消费时间范围")
    private String consumeTimeRange;

    @Schema(description = "可使用时间范围")
    private String useRangeTime;
}
