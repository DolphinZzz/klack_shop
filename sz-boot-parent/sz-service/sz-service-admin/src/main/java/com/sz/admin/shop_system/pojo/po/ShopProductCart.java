package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.io.Serial;
import com.sz.mysql.EntityChangeListener;
import java.time.LocalDateTime;

/**
* <p>
* 购物车表
* </p>
*
* @author car
* @since 2026-01-26
*/
@Data
@Table(value = "shop_product_cart", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "购物车表")
public class ShopProductCart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="购物车id")
    private Long id;

    @Schema(description ="用户id")
    private Long uid;

    @Schema(description ="商品id")
    private Long productId;

    @Schema(description ="规格id")
    private Long skuId;

    @Schema(description ="商品数量")
    private Integer cartNum;

    @Schema(description ="秒杀商品id")
    private Integer seckillId;

    @Schema(description ="砍价id")
    private Integer bargainId;

    @Schema(description ="创建时间")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    private LocalDateTime updateTime;

    @Schema(description ="状态")
    private Integer status;

}