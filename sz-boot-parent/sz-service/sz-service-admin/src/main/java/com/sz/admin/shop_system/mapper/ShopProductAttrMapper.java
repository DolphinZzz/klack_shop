package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopProductAttrDTO;
import com.sz.admin.shop_system.pojo.po.ShopProductAttr;
import com.sz.admin.shop_system.pojo.vo.ShopProductAttrVO;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 商品属性表 Mapper 接口
 * </p>
 *
 * @author car
 * @since 2025-12-04
 */
public interface ShopProductAttrMapper extends BaseMapper<ShopProductAttr> {

    List<ShopProductAttrVO> selectAttrList(@Param("dto") ShopProductAttrDTO.ListQuery dto);

    List<ShopProductAttr> selectByProductIdAndType(@Param("productId") Long productId, @Param("type") Integer type);

    List<ShopProductAttr> selectByProductId(@Param("productId") Long productId);

    int deleteByProductId(@Param("productId") Long productId);

    int deleteByProductIds(@Param("productIds") Collection<? extends Serializable> productIds);
}
