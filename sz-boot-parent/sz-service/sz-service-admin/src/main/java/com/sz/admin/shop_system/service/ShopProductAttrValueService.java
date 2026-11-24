package com.sz.admin.shop_system.service;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopProductAttrValue;

import java.util.List;

/**
 * <p>
 * 商品属性值表 Service
 * </p>
 *
 * @author car
 * @since 2025-12-04
 */
public interface ShopProductAttrValueService extends IService<ShopProductAttrValue> {

    List<ShopProductAttrValue> getListByProductId(Long id);
}
