package com.sz.admin.shop_system.pojo.page;

import com.sz.admin.shop_system.pojo.po.ShopProduct;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleFrontVO;
import com.sz.admin.shop_system.pojo.po.ShopProductAttr;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author car
 * @create 2025/12/4
 */
@Data
@Accessors(chain = true)
@Schema(description = "商品详情")
public class ProductDetailResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品信息")
    private ShopProduct productInfo;

    @Schema(description = "产品属性")
    private List<ProductAttrResponse> productAttr;

    @Schema(description = "商品属性详情")
    private HashMap<String, Object> productValue;

    @Schema(description = "秒杀信息")
    private ShopFlashSaleFrontVO flashSale;

}
