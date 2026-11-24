package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopHomeNewsCategoryDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopHomeNewsCategory;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewsCategoryVO;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;

import java.util.List;

/**
 * 资讯分类 Service
 *
 * @author car
 * @since 2026-03-28
 */
public interface ShopHomeNewsCategoryService extends IService<ShopHomeNewsCategory> {

    void create(ShopHomeNewsCategoryDTO.Create dto);

    void update(ShopHomeNewsCategoryDTO.Update dto);

    PageResult<ShopHomeNewsCategoryVO> page(ShopHomeNewsCategoryDTO.ListQuery dto);

    List<ShopHomeNewsCategoryVO> list(ShopHomeNewsCategoryDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopHomeNewsCategoryVO detail(Object id);

    List<ShopHomeNewsCategoryVO> frontList();
}
