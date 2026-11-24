package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopHomeNewsDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopHomeNews;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewsVO;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;

import java.util.List;

/**
 * 首页资讯 Service
 *
 * @author car
 * @since 2026-03-28
 */
public interface ShopHomeNewsService extends IService<ShopHomeNews> {

    void create(ShopHomeNewsDTO.Create dto);

    void update(ShopHomeNewsDTO.Update dto);

    PageResult<ShopHomeNewsVO> page(ShopHomeNewsDTO.ListQuery dto);

    List<ShopHomeNewsVO> list(ShopHomeNewsDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopHomeNewsVO detail(Object id);

    List<ShopHomeNewsVO> frontList(Integer size);

    PageResult<ShopHomeNewsVO> frontPage(ShopHomeNewsDTO.FrontPage dto);

    List<ShopHomeNewsVO> hotList(Integer size);

    ShopHomeNewsVO frontDetail(Object id);
}
