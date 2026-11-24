package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "PC端订单详情VO")
public class ShopFrontOrderDetailVO extends ShopFrontOrderVO {

    @Schema(description = "收货人")
    private String realName;

    @Schema(description = "手机号")
    private String userPhone;

    @Schema(description = "收货地址")
    private String userAddress;

    @Schema(description = "支付方式")
    private String payType;

    @Schema(description = "运费")
    private String freightTotalFee;

    @Schema(description = "快递单号")
    private String expressNo;

    @Schema(description = "快递公司名称")
    private String expressName;

    @Schema(description = "订单状态记录")
    private List<ShopOrderStatusVO> orderStatusList;
}
