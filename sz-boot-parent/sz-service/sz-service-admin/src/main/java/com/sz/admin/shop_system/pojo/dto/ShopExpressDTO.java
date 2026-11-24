package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * ShopExpressDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopExpressDTO {

    /**
     * <p>
     * ShopExpress添加DTO
     * </p>
     *
     * @author car
     * @since 2026-01-22
     */
    @Data
    @Schema(description = "ShopExpress添加DTO")
    public static class Create {

       @Schema(description =  "编号")
       private String code;

       @Schema(description =  "快递名")
       private String expressName;

       @Schema(description =  "排序")
       private Integer sort;

    }

    /**
     * <p>
     * ShopExpress修改DTO
     * </p>
     *
     * @author car
     * @since 2026-01-22
     */
    @Data
    @Schema(description = "ShopExpress修改DTO")
    public static class Update {

        @Schema(description =  "id")
        private Long id;

        @Schema(description =  "编号")
        private String code;

        @Schema(description =  "快递名")
        private String expressName;

        @Schema(description =  "排序")
        private Integer sort;

    }

    /**
     * <p>
     * ShopExpress查询DTO
     * </p>
     *
     * @author car
     * @since 2026-01-22
     */
    @Data
    @Schema(description = "ShopExpress查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "编号")
        private String code;

        @Schema(description =  "快递名")
        private String expressName;

    }
}
