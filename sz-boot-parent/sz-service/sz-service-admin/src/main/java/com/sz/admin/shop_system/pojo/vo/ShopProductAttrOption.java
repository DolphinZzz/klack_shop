package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author car
 * @create 2026/1/16
 */
@Data
public class ShopProductAttrOption {
    @Schema(description = "属性值id")
    private Long attrId;

    @Schema(description = "商品id")
    private Long productId;

    @Schema(description = "属性值")
    private String optionName;
}
