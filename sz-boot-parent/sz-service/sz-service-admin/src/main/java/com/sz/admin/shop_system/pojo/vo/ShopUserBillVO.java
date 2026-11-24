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
 * ShopUserBill返回vo
 * </p>
 *
 * @author sz-admin
 * @since 2025-11-26
 */
@Data
@Schema(description = "ShopUserBill返回vo")
public class ShopUserBillVO {

    @ExcelIgnore
    @Schema(description =  "id")
    private Long id;

    @ExcelProperty(value = "用户id")
    @Schema(description =  "用户id")
    private Long uid;

    @ExcelIgnore
    @Schema(description = "账号")
    private String account;

    @ExcelIgnore
    @Schema(description = "真实姓名")
    private String realName;

    @ExcelProperty(value = "收支")
    @Schema(description =  "收支")
    private Integer pm;

    @ExcelProperty(value = "账单标题")
    @Schema(description =  "账单标题")
    private String title;

    @ExcelProperty(value = "明细种类")
    @Schema(description =  "明细种类")
    private String category;

    @ExcelProperty(value = "明细类型")
    @Schema(description =  "明细类型")
    private String type;

    @ExcelProperty(value = "明细数字")
    @Schema(description =  "明细数字")
    private BigDecimal number;

    @ExcelProperty(value = "剩余")
    @Schema(description =  "剩余")
    private BigDecimal balance;

    @ExcelProperty(value = "备注")
    @Schema(description =  "备注")
    private String mark;

    @ExcelIgnore
    @Schema(description =  "创建时间")
    private LocalDateTime createTime;

    @ExcelIgnore
    @Schema(description =  "更新时间")
    private LocalDateTime updateTime;

    @ExcelProperty(value = "状态")
    @DictFormat(dictType = "biz_status")
    @Schema(description =  "状态")
    private Integer status;

}
