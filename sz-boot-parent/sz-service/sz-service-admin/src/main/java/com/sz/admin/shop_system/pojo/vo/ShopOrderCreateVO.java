package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author car
 * @create 2026/2/12
 */
@Data
@Schema(description = "订单创建返回VO")
public class ShopOrderCreateVO {
    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单类型")
    private Integer orderType;

    @Schema(description = "支付金额")
    private String payPrice;
}
