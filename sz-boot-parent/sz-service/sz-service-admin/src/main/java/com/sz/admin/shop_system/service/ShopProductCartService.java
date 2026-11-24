package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopProductCartDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopProductCart;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopProductCartVO;

/**
 * <p>
 * 购物车表 Service
 * </p>
 *
 * @author car
 * @since 2026-01-26
 */
public interface ShopProductCartService extends IService<ShopProductCart> {

    void create(ShopProductCartDTO.Create dto,Long userId);

    void update(ShopProductCartDTO.Update dto, Long userId);

    PageResult<ShopProductCartVO> page(ShopProductCartDTO.ListQuery dto);

    void remove(SelectIdsDTO dto, Long userId);

    Integer count(Long userId);

}
