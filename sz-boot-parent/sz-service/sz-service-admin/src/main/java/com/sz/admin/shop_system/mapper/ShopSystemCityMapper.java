package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.po.ShopSystemCity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShopSystemCityMapper extends BaseMapper<ShopSystemCity> {

    List<ShopSystemCity> selectByLevelAndParentId(@Param("level") Integer level, @Param("parentId") Integer parentId);
}
