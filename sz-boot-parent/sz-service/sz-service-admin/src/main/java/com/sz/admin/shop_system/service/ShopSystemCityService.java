package com.sz.admin.shop_system.service;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopSystemCity;
import com.sz.admin.shop_system.pojo.vo.ShopSystemCityVO;

import java.util.List;

public interface ShopSystemCityService extends IService<ShopSystemCity> {

    List<ShopSystemCityVO> listByParentAndRegionType(Integer parentId, Integer regionType);
}
