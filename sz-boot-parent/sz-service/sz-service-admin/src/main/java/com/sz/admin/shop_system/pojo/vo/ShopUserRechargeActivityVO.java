package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * ShopUserRechargeActivity返回vo
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
@Data
@Schema(description = "ShopUserRechargeActivity返回vo")
public class ShopUserRechargeActivityVO {

    @Schema(description =  "充值活动id")
    private Long id;

    @Schema(description =  "活动名称")
    private String activityName;

    @Schema(description =  "充值金额")
    private BigDecimal price;

    @Schema(description =  "赠送金额")
    private BigDecimal givePrice;

}