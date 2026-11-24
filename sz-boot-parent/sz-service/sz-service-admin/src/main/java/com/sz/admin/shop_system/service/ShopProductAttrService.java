package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopProductAttrDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopProductAttr;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopProductAttrVO;

/**
 * <p>
 * 商品属性表 Service
 * </p>
 *
 * @author car
 * @since 2025-12-04
 */
public interface ShopProductAttrService extends IService<ShopProductAttr> {

    void create(ShopProductAttrDTO.Create dto);

    void update(ShopProductAttrDTO.Update dto);

    PageResult<ShopProductAttrVO> page(ShopProductAttrDTO.ListQuery dto);

    List<ShopProductAttrVO> list(ShopProductAttrDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopProductAttrVO detail(Object id);

    List<ShopProductAttr> getListByProductIdAndType(Long id, Integer productTypeNormal);
}
