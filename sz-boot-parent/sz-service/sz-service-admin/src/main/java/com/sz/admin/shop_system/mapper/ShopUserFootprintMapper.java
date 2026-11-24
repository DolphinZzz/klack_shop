package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.po.ShopUserFootprint;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserFootprintVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShopUserFootprintMapper extends BaseMapper<ShopUserFootprint> {

    ShopUserFootprint selectActiveByUidAndProductId(@Param("uid") Long uid, @Param("productId") Long productId);

    List<ShopFrontUserFootprintVO> selectActiveFrontListByUid(@Param("uid") Long uid);

}
