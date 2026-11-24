package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * ShopOrderAfterSaleDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopOrderAfterSaleDTO {

    @Data
    @Schema(description = "管理端售后列表查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description = "售后单号")
        private String serviceNo;

        @Schema(description = "订单号")
        private String orderNo;

        @Schema(description = "售后类型")
        private Integer afterSaleType;

        @Schema(description = "售后状态")
        private Integer status;
    }

    @Data
    @Schema(description = "管理端审核售后DTO")
    public static class Audit {

        @Schema(description = "售后单ID")
        private Long id;

        @Schema(description = "是否通过")
        private Boolean approved;

        @Schema(description = "审核备注")
        private String auditRemark;
    }

    @Data
    @Schema(description = "管理端确认收货退款DTO")
    public static class Receive {

        @Schema(description = "售后单ID")
        private Long id;

        @Schema(description = "收货备注")
        private String receiveRemark;
    }

    @Data
    @Schema(description = "PC端申请售后DTO")
    public static class FrontApply {

        @Schema(description = "订单号")
        private String orderNo;

        @Schema(description = "售后类型 1仅退款 2退货退款")
        private Integer afterSaleType;

        @Schema(description = "申请金额")
        private BigDecimal applyAmount;

        @Schema(description = "售后原因")
        private String reason;

        @Schema(description = "用户备注")
        private String userRemark;
    }

    @Data
    @Schema(description = "PC端售后列表查询DTO")
    public static class FrontList extends PageQuery {

        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "关键词，支持售后单号/订单号")
        private String keyword;
    }

    @Data
    @Schema(description = "PC端填写退货物流DTO")
    public static class FrontReturn {

        @Schema(description = "售后单ID")
        private Long id;

        @Schema(description = "退货快递公司")
        private String returnExpressName;

        @Schema(description = "退货快递单号")
        private String returnExpressNo;

        @Schema(description = "退货备注")
        private String returnRemark;
    }
}
