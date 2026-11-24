package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * ShopFlashSaleDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopFlashSaleDTO {

    @Data
    @Schema(description = "秒杀营销列表DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "商品上架状态")
        private Integer productShowStatus;

        @Schema(description = "秒杀状态")
        private Integer flashStatus;
    }

    @Data
    @Schema(description = "秒杀营销更新DTO")
    public static class Update {

        @Schema(description = "秒杀记录id")
        private Long id;

        @Schema(description = "商品id")
        private Long productId;

        @Schema(description = "浏览量")
        private Integer browse;

        @Schema(description = "虚拟浏览量")
        private Integer fictiBrowse;

        @Schema(description = "销量")
        private Integer sales;

        @Schema(description = "商品上架状态")
        private Integer productShowStatus;

        @Schema(description = "是否秒杀商品")
        private Integer isSeckill;

        @Schema(description = "活动排序标记")
        private String activity;

        @Schema(description = "秒杀状态")
        private Integer flashStatus;

        @Schema(description = "商品默认秒杀价格")
        private BigDecimal flashPrice;

        @Schema(description = "每人限购")
        private Integer limitPerUser;

        @Schema(description = "保留秒数")
        private Integer reserveSeconds;

        @Schema(description = "开始时间")
        private LocalDateTime startTime;

        @Schema(description = "结束时间")
        private LocalDateTime endTime;

        @Schema(description = "SKU秒杀配置")
        private List<Sku> skuList;
    }

    @Data
    @Schema(description = "秒杀SKU配置DTO")
    public static class Sku {

        @Schema(description = "SKU id")
        private Long skuId;

        @Schema(description = "SKU秒杀价")
        private BigDecimal flashPrice;
    }

    @Data
    @Schema(description = "秒杀抢购DTO")
    public static class Reserve {

        @Schema(description = "商品id")
        private Long productId;

        @Schema(description = "规格id")
        private Long skuId;

        @Schema(description = "购买数量")
        private Integer cartNum;
    }
}
