package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "PC端我的积分汇总VO")
public class ShopFrontUserIntegralSummaryVO {

    @Schema(description = "当前积分")
    private Integer currentIntegral;

    @Schema(description = "累计积分")
    private Integer totalGainIntegral;

    @Schema(description = "消耗积分")
    private Integer totalUseIntegral;

    @Schema(description = "冻结积分")
    private Integer freezeIntegral;
}
