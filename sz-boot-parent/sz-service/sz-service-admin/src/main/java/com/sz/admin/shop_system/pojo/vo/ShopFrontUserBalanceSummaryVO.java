package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "PC端我的余额汇总VO")
public class ShopFrontUserBalanceSummaryVO {

    @Schema(description = "账户余额")
    private BigDecimal nowMoney;

    @Schema(description = "累计充值")
    private BigDecimal rechargeAmount;

    @Schema(description = "累计消费")
    private BigDecimal consumeAmount;
}
