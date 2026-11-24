package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * ShopCategoryDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopCategoryDTO {

    /**
     * <p>
     * ShopCategory添加DTO
     * </p>
     *
     * @author car
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopCategory添加DTO")
    public static class Create {

       @Schema(description =  "父id")
       private Integer pid;

       @Schema(description =  "分类名称")
       private String cateName;

       @Schema(description =  "排序")
       private Integer sort;

    }

    /**
     * <p>
     * ShopCategory修改DTO
     * </p>
     *
     * @author car
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopCategory修改DTO")
    public static class Update {

        @Schema(description =  "分类id")
        private Long id;

        @Schema(description =  "父id")
        private Integer pid;

        @Schema(description =  "分类名称")
        private String cateName;

        @Schema(description =  "排序")
        private Integer sort;

    }

    /**
     * <p>
     * ShopCategory查询DTO
     * </p>
     *
     * @author car
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopCategory查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "分类名称")
        private String cateName;

    }
}
