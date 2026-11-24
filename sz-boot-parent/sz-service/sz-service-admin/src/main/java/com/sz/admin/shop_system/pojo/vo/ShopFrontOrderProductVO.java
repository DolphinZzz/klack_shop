package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "PC端订单商品信息VO")
public class ShopFrontOrderProductVO {

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "规格ID")
    private Long skuId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String image;

    @Schema(description = "规格描述")
    private String sku;

    @Schema(description = "单价")
    private String price;

    @Schema(description = "购买数量")
    private Integer cartNum;
}
