package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.po.ShopProductAttrValue;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 商品属性值表 Mapper 接口
 * </p>
 *
 * @author car
 * @since 2025-12-04
 */
public interface ShopProductAttrValueMapper extends BaseMapper<ShopProductAttrValue> {

    List<ShopProductAttrValue> selectActiveByProductId(@Param("productId") Long productId);

    List<ShopProductAttrValue> selectByProductId(@Param("productId") Long productId);

    int deleteByProductId(@Param("productId") Long productId);

    int deleteByProductIds(@Param("productIds") Collection<? extends Serializable> productIds);

    int deductStockOptimistic(@Param("id") Long id,
                              @Param("newStock") Integer newStock,
                              @Param("newVersion") Integer newVersion,
                              @Param("minStock") Integer minStock,
                              @Param("expectedVersion") Integer expectedVersion);

    int restoreStockOptimistic(@Param("id") Long id,
                               @Param("newStock") Integer newStock,
                               @Param("newVersion") Integer newVersion,
                               @Param("expectedVersion") Integer expectedVersion);
}
