package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * ShopHomeNewsCategoryDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopHomeNewsCategoryDTO {

    /**
     * 资讯分类新增DTO
     *
     * @author car
     * @since 2026-03-28
     */
    @Data
    @Schema(description = "资讯分类新增DTO")
    public static class Create {

        @Schema(description = "分类名称")
        private String categoryName;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "状态：0-正常 1-禁用")
        private Integer status;

        @Schema(description = "备注")
        private String remark;
    }

    /**
     * 资讯分类修改DTO
     *
     * @author car
     * @since 2026-03-28
     */
    @Data
    @Schema(description = "资讯分类修改DTO")
    public static class Update {

        @Schema(description = "分类id")
        private Long id;

        @Schema(description = "分类名称")
        private String categoryName;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "状态：0-正常 1-禁用")
        private Integer status;

        @Schema(description = "备注")
        private String remark;
    }

    /**
     * 资讯分类查询DTO
     *
     * @author car
     * @since 2026-03-28
     */
    @Data
    @Schema(description = "资讯分类查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description = "分类名称")
        private String categoryName;

        @Schema(description = "状态：0-正常 1-禁用")
        private Integer status;
    }
}
