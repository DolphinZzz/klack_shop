package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopHomeBannerDTO;
import com.sz.admin.shop_system.pojo.po.ShopHomeBanner;
import com.sz.admin.shop_system.pojo.vo.ShopHomeBannerVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 首页轮播 Mapper
 *
 * @author car
 * @since 2026-02-26
 */
public interface ShopHomeBannerMapper extends BaseMapper<ShopHomeBanner> {

    List<ShopHomeBannerVO> selectBannerList(@Param("query") ShopHomeBannerDTO.ListQuery query);

    List<ShopHomeBannerVO> selectFrontBannerList(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
