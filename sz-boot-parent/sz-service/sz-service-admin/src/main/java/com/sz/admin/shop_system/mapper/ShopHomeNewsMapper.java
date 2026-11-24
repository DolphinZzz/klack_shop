package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopHomeNewsDTO;
import com.sz.admin.shop_system.pojo.po.ShopHomeNews;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewsVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 首页资讯 Mapper
 *
 * @author car
 * @since 2026-03-28
 */
public interface ShopHomeNewsMapper extends BaseMapper<ShopHomeNews> {

    List<ShopHomeNewsVO> selectNewsList(@Param("query") ShopHomeNewsDTO.ListQuery query);

    List<ShopHomeNewsVO> selectFrontNewsList(@Param("now") LocalDateTime now, @Param("limit") int limit);

    List<ShopHomeNewsVO> selectFrontNewsPageList(@Param("now") LocalDateTime now, @Param("categoryId") Long categoryId);

    List<ShopHomeNewsVO> selectHotNewsList(@Param("now") LocalDateTime now, @Param("limit") int limit);

    ShopHomeNews selectFrontNewsById(@Param("id") Object id, @Param("now") LocalDateTime now);
}
