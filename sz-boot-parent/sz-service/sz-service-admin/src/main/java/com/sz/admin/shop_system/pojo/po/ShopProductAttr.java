package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.io.Serial;
import com.sz.mysql.EntityChangeListener;

/**
* <p>
* 商品属性表
* </p>
*
* @author car
* @since 2025-12-04
*/
@Data
@Table(value = "shop_product_attr", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "商品属性表")
@Accessors(chain = true)
public class ShopProductAttr implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="主键")
    private Long id;

    @Schema(description ="商品id")
    private Long productId;

    @Schema(description ="属性名")
    private String attrName;

    @Schema(description ="属性值")
    private String attrValues;

    @Schema(description ="活动类型，0商品，1秒杀，2砍价")
    private Integer type;

    @Schema(description ="是否删除，0否，1是")
    private Integer isDel;

}
