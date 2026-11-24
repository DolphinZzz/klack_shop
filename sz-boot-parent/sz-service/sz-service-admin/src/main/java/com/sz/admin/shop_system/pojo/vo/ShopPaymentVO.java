package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author car
 * @create 2026/2/25
 */
@Data
@Schema(description = "订单支付VO")
public class ShopPaymentVO {
    @Schema(description = "订单号")
    private String orderNo;
    @Schema(description = "支付类型")
    private String payType;
    @Schema(description = "支付状态")
    private Boolean status;
}
