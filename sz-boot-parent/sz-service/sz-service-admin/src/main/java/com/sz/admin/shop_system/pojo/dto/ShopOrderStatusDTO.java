package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * ShopOrderStatusDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopOrderStatusDTO {

    /**
     * <p>
     * ShopOrderStatus添加DTO
     * </p>
     *
     * @author car
     * @since 2026-01-29
     */
    @Data
    @Schema(description = "ShopOrderStatus添加DTO")
    public static class Create {

       @Schema(description =  "订单id")
       private Long oid;

       @Schema(description =  "操作类型")
       private String changeType;

       @Schema(description =  "操作备注")
       private String changeMessage;

    }

    /**
     * <p>
     * ShopOrderStatus修改DTO
     * </p>
     *
     * @author car
     * @since 2026-01-29
     */
    @Data
    @Schema(description = "ShopOrderStatus修改DTO")
    public static class Update {

        @Schema(description =  "订单状态id")
        private Long id;

    }

    /**
     * <p>
     * ShopOrderStatus查询DTO
     * </p>
     *
     * @author car
     * @since 2026-01-29
     */
    @Data
    @Schema(description = "ShopOrderStatus查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "订单id")
        private Long oid;

    }
}
