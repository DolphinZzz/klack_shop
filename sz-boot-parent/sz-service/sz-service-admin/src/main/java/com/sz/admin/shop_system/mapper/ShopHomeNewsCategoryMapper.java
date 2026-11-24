package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopHomeNewsCategoryDTO;
import com.sz.admin.shop_system.pojo.po.ShopHomeNewsCategory;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewsCategoryVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资讯分类 Mapper
 *
 * @author car
 * @since 2026-03-28
 */
public interface ShopHomeNewsCategoryMapper extends BaseMapper<ShopHomeNewsCategory> {

    List<ShopHomeNewsCategoryVO> selectCategoryList(@Param("query") ShopHomeNewsCategoryDTO.ListQuery query);

    List<ShopHomeNewsCategoryVO> selectFrontCategoryList();
}
