package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopHomeBannerDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopHomeBanner;
import com.sz.admin.shop_system.pojo.vo.ShopHomeBannerVO;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;

import java.util.List;

/**
 * 首页轮播 Service
 *
 * @author car
 * @since 2026-02-26
 */
public interface ShopHomeBannerService extends IService<ShopHomeBanner> {

    void create(ShopHomeBannerDTO.Create dto);

    void update(ShopHomeBannerDTO.Update dto);

    PageResult<ShopHomeBannerVO> page(ShopHomeBannerDTO.ListQuery dto);

    List<ShopHomeBannerVO> list(ShopHomeBannerDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopHomeBannerVO detail(Object id);

    List<ShopHomeBannerVO> frontList(Integer size);
}
