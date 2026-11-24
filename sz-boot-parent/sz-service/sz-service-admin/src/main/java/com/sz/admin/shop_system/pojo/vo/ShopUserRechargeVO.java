package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import com.sz.excel.annotation.DictFormat;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * ShopUserRecharge返回vo
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
@Data
@Schema(description = "ShopUserRecharge返回vo")
public class ShopUserRechargeVO {

    @ExcelIgnore
    @Schema(description =  "id")
    private Long id;

    @ExcelProperty(value = "充值用户id")
    @Schema(description =  "充值用户id")
    private Long uid;

    @ExcelProperty(value = "充值活动id")
    @Schema(description =  "充值活动id")
    private Long payActivityId;

    @ExcelProperty(value = "充值订单id")
    @Schema(description =  "充值订单id")
    private Long orderId;

    @ExcelProperty(value = "充值金额")
    @Schema(description =  "充值金额")
    private BigDecimal price;

    @ExcelProperty(value = "赠送金额")
    @Schema(description =  "赠送金额")
    private BigDecimal givePrice;

    @ExcelProperty(value = "充值类型")
    @DictFormat(dictType = "recharge_type")
    @Schema(description =  "充值类型")
    private String rechargeType;

    @ExcelProperty(value = "充值时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description =  "充值时间")
    private LocalDateTime payTime;

    @ExcelProperty(value = "退款金额")
    @Schema(description =  "退款金额")
    private BigDecimal refundPrice;

}