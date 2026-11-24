package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * ShopProductCoupon返回vo
 * </p>
 *
 * @author car
 * @since 2026-01-30
 */
@Data
@Schema(description = "ShopProductCoupon返回vo")
public class ShopProductCouponVO {

    @Schema(description =  "优惠券id")
    private Long id;

    @Schema(description =  "优惠券名称")
    private String name;

    @Schema(description =  "优惠券面值")
    private BigDecimal money;

    @Schema(description =  "发放总数")
    private Integer total;

    @Schema(description =  "剩余数量")
    private Integer lastTotal;

    @Schema(description =  "优惠券类型")
    private Integer type;

    @Schema(description =  "所属商品id")
    private String cateKey;

    @Schema(description =  "最低消费")
    private BigDecimal minMoney;

    @Schema(description ="领取时间")
    private LocalDateTime couponStartTime;

    @Schema(description ="结束时间")
    private LocalDateTime couponEndTime;

    @Schema(description ="可使用开始时间")
    private LocalDateTime useStartTime;

    @Schema(description ="可使用结束时间")
    private LocalDateTime useEndTime;

    @Schema(description = "当前用户已领取记录ID（shop_product_coupon_user.id）")
    private Long couponUserId;

    @Schema(description = "当前用户是否已领取，0未领取 1已领取")
    private Integer received;

    @Schema(description = "适用商品名称")
    private String productNames;

    @Schema(description = "适用商品主图")
    private String productImage;

}
