package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * ShopUserRechargeActivityDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopUserRechargeActivityDTO {

    /**
     * <p>
     * ShopUserRechargeActivity添加DTO
     * </p>
     *
     * @author car
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopUserRechargeActivity添加DTO")
    public static class Create {

       @Schema(description =  "活动名称")
       private String activityName;

       @Schema(description =  "充值金额")
       private BigDecimal price;

       @Schema(description =  "赠送金额")
       private BigDecimal givePrice;

    }

    /**
     * <p>
     * ShopUserRechargeActivity修改DTO
     * </p>
     *
     * @author car
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopUserRechargeActivity修改DTO")
    public static class Update {

        @Schema(description =  "充值活动id")
        private Long id;

        @Schema(description =  "活动名称")
        private String activityName;

        @Schema(description =  "充值金额")
        private BigDecimal price;

        @Schema(description =  "赠送金额")
        private BigDecimal givePrice;

    }

    /**
     * <p>
     * ShopUserRechargeActivity查询DTO
     * </p>
     *
     * @author car
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopUserRechargeActivity查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "活动名称")
        private String activityName;

    }
}
