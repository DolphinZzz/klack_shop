package com.sz.admin.shop_system.pojo.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sz.admin.shop_system.util.StringOrArrayToStringDeserializer;
import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * ShopHomeBannerDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopHomeBannerDTO {

    /**
     * 首页轮播新增DTO
     *
     * @author car
     * @since 2026-02-26
     */
    @Data
    @Schema(description = "首页轮播新增DTO")
    public static class Create {

        @Schema(description = "标题")
        private String title;

        @Schema(description = "图片")
        @JsonDeserialize(using = StringOrArrayToStringDeserializer.class)
        private String image;

        @Schema(description = "跳转链接")
        private String link;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "状态：0-正常 1-禁用")
        private Integer status;

        @Schema(description = "开始时间")
        private LocalDateTime startTime;

        @Schema(description = "结束时间")
        private LocalDateTime endTime;

        @Schema(description = "备注")
        private String remark;
    }

    /**
     * 首页轮播修改DTO
     *
     * @author car
     * @since 2026-02-26
     */
    @Data
    @Schema(description = "首页轮播修改DTO")
    public static class Update {

        @Schema(description = "轮播id")
        private Long id;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "图片")
        @JsonDeserialize(using = StringOrArrayToStringDeserializer.class)
        private String image;

        @Schema(description = "跳转链接")
        private String link;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "状态：0-正常 1-禁用")
        private Integer status;

        @Schema(description = "开始时间")
        private LocalDateTime startTime;

        @Schema(description = "结束时间")
        private LocalDateTime endTime;

        @Schema(description = "备注")
        private String remark;
    }

    /**
     * 首页轮播查询DTO
     *
     * @author car
     * @since 2026-02-26
     */
    @Data
    @Schema(description = "首页轮播查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description = "标题")
        private String title;

        @Schema(description = "状态：0-正常 1-禁用")
        private Integer status;
    }
}
