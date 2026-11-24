package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * ShopExpressTemplateDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopExpressTemplateDTO {

    /**
     * <p>
     * ShopExpressTemplate添加DTO
     * </p>
     *
     * @author car
     * @since 2026-01-22
     */
    @Data
    @Schema(description = "ShopExpressTemplate添加DTO")
    public static class Create {

       @Schema(description =  "快递名称")
       private String name;

       @Schema(description =  "快递id")
       private Long expressId;

       @Schema(description =  "价格")
       private BigDecimal expressPrice;

       @Schema(description =  "是否删除 0-否 1-是")
       private Integer isDel;

    }

    /**
     * <p>
     * ShopExpressTemplate修改DTO
     * </p>
     *
     * @author car
     * @since 2026-01-22
     */
    @Data
    @Schema(description = "ShopExpressTemplate修改DTO")
    public static class Update {

        @Schema(description =  "快递模板id")
        private Long id;

        @Schema(description =  "快递名称")
        private String name;

        @Schema(description =  "快递id")
        private Long expressId;

        @Schema(description =  "价格")
        private BigDecimal expressPrice;

        @Schema(description =  "是否删除 0-否 1-是")
        private Integer isDel;

    }

    /**
     * <p>
     * ShopExpressTemplate查询DTO
     * </p>
     *
     * @author car
     * @since 2026-01-22
     */
    @Data
    @Schema(description = "ShopExpressTemplate查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "快递名称")
        private String name;

        @Schema(description = "快递编号")
        private String code;

        @Schema(description =  "价格")
        private BigDecimal expressPrice;

    }
}
