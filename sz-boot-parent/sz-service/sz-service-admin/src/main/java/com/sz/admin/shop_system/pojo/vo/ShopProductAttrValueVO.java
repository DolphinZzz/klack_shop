package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 商品属性值表
 * </p>
 *
 * @author car
 * @since 2025-12-04
 */
@Data
@Schema(description = "商品属性值表")
public class ShopProductAttrValueVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键id")
    private Long id;

    @Schema(description = "商品id")
    private Integer productId;

    @Schema(description = "商品属性索引值 (attr_value|attr_value[|....])")
    private String sku;

    @Schema(description = "属性对应的库存")
    private Integer stock;

    @Schema(description = "属性金额")
    private BigDecimal price;

    @Schema(description = "SKU秒杀价")
    private BigDecimal flashPrice;

    @Schema(description = "图片")
    private String image;

    @Schema(description = "成本价")
    private BigDecimal cost;

    @Schema(description = "商品条码")
    private String barCode;

    @Schema(description = "原价")
    private BigDecimal otPrice;

    @Schema(description = "重量")
    private BigDecimal weight;

    @Schema(description = "体积")
    private BigDecimal volume;

    @Schema(description = "是否删除，0-否，1-是")
    private Boolean isDel;

    @Schema(description = "是否默认")
    private Boolean isDefault;

    @Schema(description = "json属性名-值组合")
    private String attrValue;
}
