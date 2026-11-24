package com.sz.admin.shop_system.pojo.page;

import com.sz.admin.shop_system.pojo.po.ShopProductAttr;
import com.sz.admin.shop_system.pojo.vo.ShopProductAttrVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author car
 * @create 2025/12/5
 */
@Data
@Accessors(chain = true)
@Schema(description = "商品详情")
public class ProductAttrResponse implements Serializable {
    private final static long serialVersionUID = 1L;

    @Schema(description = "属性id")
    private Long id;
    @Schema(description = "属性名")
    private String attrName;
    @Schema(description = "属性规格")
    private List<ShopProductAttrVO> optionList;

}
