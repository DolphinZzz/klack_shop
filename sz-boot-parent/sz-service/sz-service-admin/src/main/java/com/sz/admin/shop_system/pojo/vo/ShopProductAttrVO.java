package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * <p>
 * ShopProductAttr返回vo
 * </p>
 *
 * @author car
 * @since 2025-12-04
 */
@Data
@Schema(description = "ShopProductAttr返回vo")
public class ShopProductAttrVO {
    private Long id;

    @Schema(description =  "商品id")
    private Long productId;

    @Schema(description =  "属性名")
    private String attrName;

    @Schema(description =  "是否删除，0否，1是")
    private Integer isDel;

    @Schema(description = "属性值")
    private List<ShopProductAttrOption> optionList;
}
