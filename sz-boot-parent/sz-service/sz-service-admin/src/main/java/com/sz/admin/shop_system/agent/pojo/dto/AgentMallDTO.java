package com.sz.admin.shop_system.agent.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Agent 内网商城接口请求 DTO
 */
public class AgentMallDTO {

    @Data
    @Schema(description = "智能客服聊天确认直购下单请求")
    public static class DirectCreateOrder {

        @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long productId;

        @Schema(description = "商品SKU ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long skuId;

        @Schema(description = "购买数量", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer quantity;

        @Schema(description = "收货地址ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long addressId;

        @Schema(description = "下单幂等键（同一草稿重复提交保持不变）", requiredMode = Schema.RequiredMode.REQUIRED)
        private String idempotencyKey;
    }
}
