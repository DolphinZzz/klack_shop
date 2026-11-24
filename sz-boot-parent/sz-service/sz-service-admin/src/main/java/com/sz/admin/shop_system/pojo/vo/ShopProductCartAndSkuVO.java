package com.sz.admin.shop_system.pojo.vo;

import com.sz.admin.shop_system.pojo.po.ShopProductCart;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author car
 * @create 2026/1/29
 */
@Data
@Accessors(chain = true)
public class ShopProductCartAndSkuVO extends ShopProductCart {
    @Schema(description = "sku")
    private String sku;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "邮费")
    private BigDecimal postage;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String image;

    @Schema(description = "分类id")
    private Integer cateId;

    @Schema(description = "赠送积分")
    private BigDecimal giveIntegral;

}
