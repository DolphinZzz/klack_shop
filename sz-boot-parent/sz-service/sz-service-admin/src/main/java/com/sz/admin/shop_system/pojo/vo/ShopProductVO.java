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
 * ShopProduct返回vo
 * </p>
 *
 * @author car
 * @since 2025-12-03
 */
@Data
@Schema(description = "ShopProduct返回vo")
public class ShopProductVO {

    @ExcelIgnore
    @Schema(description =  "商品id")
    private Long id;

    @ExcelIgnore
    @Schema(description =  "商品图片")
    private String image;

    @ExcelProperty(value = "商品名称")
    @Schema(description =  "商品名称")
    private String productName;

    @ExcelProperty(value = "商品价格")
    @Schema(description =  "商品价格")
    private BigDecimal price;

    @ExcelProperty(value = "销量")
    @Schema(description =  "销量")
    private Integer sales;

    @ExcelProperty(value = "库存")
    @Schema(description =  "库存")
    private Integer stock;

    @ExcelProperty(value = "状态")
    @DictFormat(dictType = "is_show")
    @Schema(description =  "状态")
    private Integer isShow;

    @ExcelIgnore
    @Schema(description =  "开启秒杀")
    private Integer isSeckill;

    @ExcelProperty(value = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description =  "创建时间")
    private LocalDateTime createTime;

    @ExcelProperty(value = "更新时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description =  "更新时间")
    private LocalDateTime updateTime;

}