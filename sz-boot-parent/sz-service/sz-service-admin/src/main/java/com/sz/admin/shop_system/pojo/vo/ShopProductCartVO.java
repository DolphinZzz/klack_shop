package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * ShopProductCart返回vo
 * </p>
 *
 * @author car
 * @since 2026-01-26
 */
@Data
@Schema(description = "ShopProductCart返回vo")
public class ShopProductCartVO {

    @Schema(description =  "购物车id")
    private Long id;

    @Schema(description =  "商品id")
    private Long productId;

    @Schema(description = "规格id")
    private Long skuId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "图片")
    private String image;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "vip价格")
    private BigDecimal vipPrice;

    @Schema(description = "sku")
    private String sku;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description =  "商品数量")
    private Integer cartNum;

}
