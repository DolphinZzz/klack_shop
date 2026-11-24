package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "秒杀营销详情VO")
public class ShopFlashSaleDetailVO {

    private Long productId;

    private String productName;

    private String image;

    private BigDecimal price;

    private Integer stock;

    private Integer browse;

    private Integer fictiBrowse;

    private Integer sales;

    @Schema(description = "商品上架状态")
    private Integer productShowStatus;

    private Integer isSeckill;

    private String activity;

    private Integer flashStatus;

    private BigDecimal flashPrice;

    private Integer limitPerUser;

    private Integer reserveSeconds;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Schema(description = "商品属性")
    private List<ShopProductAttrVO> attrList;

    @Schema(description = "SKU列表")
    private List<ShopFlashSaleSkuVO> skuList;
}
