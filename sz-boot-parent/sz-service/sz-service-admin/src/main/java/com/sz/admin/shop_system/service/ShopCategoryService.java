package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopCategoryDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopCategory;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopCategoryVO;

/**
 * <p>
 * 分类管理 Service
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
public interface ShopCategoryService extends IService<ShopCategory> {

    void create(ShopCategoryDTO.Create dto);

    void update(ShopCategoryDTO.Update dto);

    PageResult<ShopCategoryVO> page(ShopCategoryDTO.ListQuery dto);

    List<ShopCategoryVO> list(ShopCategoryDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopCategoryVO detail(Object id);

    List<ShopCategoryVO> cateChildrenList();

    List<ShopCategory> getByPIds(List<Integer> idList);
}
