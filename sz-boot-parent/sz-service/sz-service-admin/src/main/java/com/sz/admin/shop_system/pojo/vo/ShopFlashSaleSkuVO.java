package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "秒杀SKU配置VO")
public class ShopFlashSaleSkuVO {

    @Schema(description = "SKU id")
    private Long skuId;

    @Schema(description = "SKU编码")
    private String sku;

    @Schema(description = "规格说明")
    private String attrValue;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "原价")
    private BigDecimal price;

    @Schema(description = "SKU秒杀价")
    private BigDecimal flashPrice;

    @Schema(description = "是否默认")
    private Integer isDefault;

    @Schema(description = "图片")
    private String image;
}
