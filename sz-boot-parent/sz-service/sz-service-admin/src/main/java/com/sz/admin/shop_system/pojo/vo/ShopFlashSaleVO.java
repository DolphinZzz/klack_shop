package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "秒杀营销列表VO")
public class ShopFlashSaleVO {

    private Long id;

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

    private Integer flashActive;
}
