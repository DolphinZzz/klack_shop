package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopUserFootprintDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopUserFootprint;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserFootprintVO;

import java.util.List;

public interface ShopUserFootprintService extends IService<ShopUserFootprint> {

    void record(Long userId, Long productId);

    List<ShopFrontUserFootprintVO> frontList(Long userId, ShopUserFootprintDTO.FrontList dto);
}
