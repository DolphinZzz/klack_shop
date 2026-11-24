package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;

/**
 * <p>
 * ShopExpressTemplate返回vo
 * </p>
 *
 * @author car
 * @since 2026-01-22
 */
@Data
@Schema(description = "ShopExpressTemplate返回vo")
public class ShopExpressTemplateVO {

    @ExcelIgnore
    @Schema(description =  "快递模板id")
    private Long id;

    @ExcelProperty(value = "快递名称")
    @Schema(description =  "快递名称")
    private String name;

    @ExcelProperty(value = "快递id")
    @Schema(description =  "快递id")
    private Long expressId;

    @ExcelProperty(value = "快递编号")
    @Schema(description = "快递编号")
    private String code;

    @ExcelProperty(value = "快递名")
    @Schema(description = "快递名")
    private String expressName;

    @ExcelProperty(value = "排序")
    @Schema(description = "排序")
    private Integer sort;

    @ExcelProperty(value = "创建时间")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @ExcelProperty(value = "更新时间")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @ExcelProperty(value = "价格")
    @Schema(description =  "价格")
    private BigDecimal expressPrice;

    @ExcelIgnore
    @Schema(description =  "是否删除 0-否 1-是")
    private Integer isDel;

}
