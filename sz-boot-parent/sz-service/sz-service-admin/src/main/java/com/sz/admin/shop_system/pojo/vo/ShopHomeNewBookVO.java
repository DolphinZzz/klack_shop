package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 首页新品推荐
 *
 * @author car
 * @since 2026-02-26
 */
@Data
@Schema(description = "首页新品推荐")
public class ShopHomeNewBookVO {

    @Schema(description = "商品id")
    private Long id;

    @Schema(description = "商品名")
    private String productName;

    @Schema(description = "封面图")
    private String image;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "浏览量")
    private Integer browse;

    @Schema(description = "是否精品")
    private Integer isBest;

    @Schema(description = "创建时间")
    private String createTime;
}
