package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author car
 * @create 2026/1/29
 */
@Data
@Schema(description = "未支付订单VO")
public class ShopPreOrderVO {
    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "地址id")
    private Long addressId;

    @Schema(description = "整体邮费")
    private String freightTotalFee;

    @Schema(description = "支付价格")
    private String payFee;

    @Schema(description = "是否可以使用积分")
    private Boolean isUseIntegral;

    @Schema(description = "可使用积分")
    private Integer userIntegral;

    @Schema(description = "是否可以使用优惠券")
    private Boolean isUseCoupon;

    @Schema(description = "优惠券抵扣金额")
    private String couponPrice;

    @Schema(description = "积分抵扣金额")
    private String deductionPrice;

    @Schema(description = "最终价格")
    private String finalTotalFee;

    @Schema(description = "商品数量总数")
    private Integer totalNum;

    @Schema(description = "订单类型(0普通 1秒杀)")
    private Integer orderType;

    @Schema(description = "确认页剩余秒数")
    private Long remainingSeconds;

    @Schema(description = "购物车商品")
    private List<ShopProductCartAndSkuVO> orderInfoList;

    @Schema(description = "平台优惠券")
    private List<ShopCouponAndUserVO> platCouponUserList;
}
