package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "前台秒杀商品VO")
public class ShopFlashSaleFrontVO {

    private Long id;

    private Long productId;

    private String productName;

    private String image;

    private BigDecimal price;

    private BigDecimal flashPrice;

    private Integer stock;

    private Integer sales;

    private Integer browse;

    private Integer fictiBrowse;

    private Integer limitPerUser;

    private Integer status;

    private String statusText;

    private Boolean canRush;

    private Long remainingSeconds;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
