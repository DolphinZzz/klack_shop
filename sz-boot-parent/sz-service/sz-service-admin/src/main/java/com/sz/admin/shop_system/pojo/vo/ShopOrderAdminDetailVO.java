package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "管理端订单详情VO")
public class ShopOrderAdminDetailVO extends ShopPreOrderVO {

    @Schema(description = "订单状态记录")
    private List<ShopOrderStatusVO> orderStatusList;

    @Schema(description = "快递公司id")
    private Long deliveryId;

    @Schema(description = "快递单号")
    private String expressNo;

    @Schema(description = "快递公司编码")
    private String expressCode;

    @Schema(description = "快递公司名称")
    private String expressName;
}
