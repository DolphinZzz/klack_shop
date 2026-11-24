package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopCategoryDTO;
import com.sz.admin.shop_system.pojo.po.ShopCategory;
import com.sz.admin.shop_system.pojo.vo.ShopCategoryVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* <p>
* 分类管理 Mapper 接口
* </p>
*
* @author car
* @since 2025-11-26
*/
public interface ShopCategoryMapper extends BaseMapper<ShopCategory> {

    List<ShopCategory> selectAllCategories();

    List<ShopCategoryVO> selectCategoryList(@Param("query") ShopCategoryDTO.ListQuery query);

    List<ShopCategory> selectChildrenByPid(@Param("pid") Long pid);

    List<ShopCategory> selectByPids(@Param("pids") List<Integer> pids);
}