package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopUserFavoriteDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopUserFavorite;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserFavoriteStatusVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserFavoriteVO;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;

public interface ShopUserFavoriteService extends IService<ShopUserFavorite> {

    void create(Long userId, Long productId);

    PageResult<ShopFrontUserFavoriteVO> page(Long userId, ShopUserFavoriteDTO.FrontList dto);

    void remove(SelectIdsDTO dto, Long userId);

    void removeByProductId(Long userId, Long productId);

    ShopFrontUserFavoriteStatusVO status(Long userId, Long productId);

    Integer count(Long userId);
}
