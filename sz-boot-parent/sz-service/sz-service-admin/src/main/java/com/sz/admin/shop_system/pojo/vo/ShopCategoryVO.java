package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * ShopCategory返回vo
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
@Data
@Schema(description = "ShopCategory返回vo")
public class ShopCategoryVO {

    @Schema(description =  "id")
    private Integer id;

    @Schema(description =  "父id")
    private Integer pid;

    @Schema(description =  "分类名称")
    private String cateName;

    @Schema(description =  "排序")
    private Integer sort;

    private List<ShopCategoryVO> children;

}
