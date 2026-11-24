package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * ShopUserRechargeDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopUserRechargeDTO {

    /**
     * <p>
     * ShopUserRecharge添加DTO
     * </p>
     *
     * @author car
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopUserRecharge添加DTO")
    public static class Create {
        @Schema(description = "支付方式")
        private String from;
        @Schema(description = "充值金额")
        private String price;
        @Schema(description = "充值活动id")
        private String activityId;
    }

    /**
     * <p>
     * ShopUserRecharge修改DTO
     * </p>
     *
     * @author car
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopUserRecharge修改DTO")
    public static class Update {

        @Schema(description =  "id")
        private Long id;

    }

    /**
     * <p>
     * ShopUserRecharge查询DTO
     * </p>
     *
     * @author car
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopUserRecharge查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "充值用户id")
        private Long uid;

        @Schema(description =  "充值活动id")
        private Long payActivityId;

        @Schema(description =  "充值订单id")
        private Long orderId;

        @Schema(description =  "充值金额")
        private BigDecimal price;

        @Schema(description =  "赠送金额")
        private BigDecimal givePrice;

        @Schema(description =  "充值类型")
        private String rechargeType;

        @Schema(description =  "充值时间开始")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime payTimeStart;

        @Schema(description =  "充值时间结束")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime payTimeEnd;

        @Schema(description =  "退款金额")
        private BigDecimal refundPrice;

        @Schema(description =  "是否删除 0-否 1-是")
        private Integer isDel;

    }
}
