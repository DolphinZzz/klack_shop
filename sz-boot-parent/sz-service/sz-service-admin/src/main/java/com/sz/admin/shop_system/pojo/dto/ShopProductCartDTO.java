package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * ShopProductCartDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopProductCartDTO {

    /**
     * <p>
     * ShopProductCart添加DTO
     * </p>
     *
     * @author car
     * @since 2026-01-26
     */
    @Data
    @Schema(description = "ShopProductCart添加DTO")
    public static class Create {

       @Schema(description =  "商品id")
       private Long productId;

       @Schema(description =  "规格id")
       private Long skuId;

       @Schema(description =  "商品数量")
       private Integer cartNum;

    }

    /**
     * <p>
     * ShopProductCart修改DTO
     * </p>
     *
     * @author car
     * @since 2026-01-26
     */
    @Data
    @Schema(description = "ShopProductCart修改DTO")
    public static class Update {

        @Schema(description =  "购物车id")
        private Long id;

        @Schema(description =  "用户id")
        private Long uid;

        @Schema(description =  "商品id")
        private Long productId;

        @Schema(description =  "规格id")
        private Long skuId;

        @Schema(description =  "商品数量")
        private Integer cartNum;

        @Schema(description =  "秒杀商品id")
        private Integer seckillId;

        @Schema(description =  "砍价id")
        private Integer bargainId;

        @Schema(description =  "状态")
        private Integer status;

    }

    /**
     * <p>
     * ShopProductCart查询DTO
     * </p>
     *
     * @author car
     * @since 2026-01-26
     */
    @Data
    @Schema(description = "ShopProductCart查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "用户id")
        private Long uid;

        @Schema(description =  "状态")
        private Integer status;

    }
}
