package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopExpressDTO;
import com.sz.admin.shop_system.pojo.po.ShopExpress;
import com.sz.admin.shop_system.pojo.vo.ShopExpressVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* <p>
* 快递表 Mapper 接口
* </p>
*
* @author car
* @since 2026-01-22
*/
public interface ShopExpressMapper extends BaseMapper<ShopExpress> {

    List<ShopExpressVO> selectExpressList(@Param("query") ShopExpressDTO.ListQuery query);
}