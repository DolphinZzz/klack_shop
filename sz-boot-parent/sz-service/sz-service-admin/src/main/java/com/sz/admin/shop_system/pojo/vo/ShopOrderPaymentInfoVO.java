package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单支付页信息VO")
public class ShopOrderPaymentInfoVO {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "支付金额")
    private String payPrice;

    @Schema(description = "商品总数")
    private Integer totalNum;

    @Schema(description = "支付状态：0未支付 1支付成功 2支付失败/已失效")
    private Integer paid;

    @Schema(description = "订单类型(0普通 1秒杀)")
    private Integer orderType;

    @Schema(description = "支付方式")
    private String payType;

    @Schema(description = "剩余支付秒数")
    private Long remainingSeconds;

    @Schema(description = "用户余额")
    private String nowMoney;

    @Schema(description = "是否可继续支付")
    private Boolean canPay;

    @Schema(description = "是否显示微信支付")
    private Boolean enableWxpay;

    @Schema(description = "是否显示支付宝支付")
    private Boolean enableAlipay;

    @Schema(description = "是否显示余额支付")
    private Boolean enableYue;
}
