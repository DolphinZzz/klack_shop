package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "PC端订单中心列表VO")
public class ShopFrontOrderVO {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "支付状态 0未支付 1已支付 2支付失败")
    private Integer paid;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "当前显示状态标识")
    private String displayTab;

    @Schema(description = "当前显示状态文案")
    private String displayStatus;

    @Schema(description = "订单类型(0普通 1秒杀)")
    private Integer orderType;

    @Schema(description = "订单类型文案")
    private String orderTypeLabel;

    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "支付金额")
    private String payPrice;

    @Schema(description = "商品件数")
    private Integer totalNum;

    @Schema(description = "首个商品ID")
    private Long firstProductId;

    @Schema(description = "是否可支付")
    private Boolean canPay;

    @Schema(description = "是否可确认收货")
    private Boolean canConfirmReceipt;

    @Schema(description = "是否可评价")
    private Boolean canEvaluate;

    @Schema(description = "是否可再次购买")
    private Boolean canBuyAgain;

    @Schema(description = "是否可删除")
    private Boolean canDelete;

    @Schema(description = "是否可申请售后")
    private Boolean canApplyAfterSale;

    @Schema(description = "关联售后单ID")
    private Long afterSaleId;

    @Schema(description = "售后状态")
    private Integer afterSaleStatus;

    @Schema(description = "售后状态文案")
    private String afterSaleStatusText;

    @Schema(description = "订单商品列表")
    private List<ShopFrontOrderProductVO> orderInfoList;
}
