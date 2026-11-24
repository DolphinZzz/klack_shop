package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.po.ShopUserFavorite;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserFavoriteVO;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.List;

public interface ShopUserFavoriteMapper extends BaseMapper<ShopUserFavorite> {

    ShopUserFavorite selectOneByUidAndProductId(@Param("uid") Long uid, @Param("productId") Long productId);

    ShopUserFavorite selectActiveByUidAndProductId(@Param("uid") Long uid, @Param("productId") Long productId);

    List<ShopFrontUserFavoriteVO> selectFrontFavoriteList(@Param("uid") Long uid);

    int softDeleteByUidAndIds(@Param("uid") Long uid, @Param("ids") List<? extends Serializable> ids);

    int softDeleteByUidAndProductId(@Param("uid") Long uid, @Param("productId") Long productId);

    long countActiveByUid(@Param("uid") Long uid);

}
