package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopOrderStatusDTO;
import com.sz.admin.shop_system.pojo.po.ShopOrderStatus;
import com.sz.admin.shop_system.pojo.vo.ShopOrderStatusVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* <p>
* 订单状态表 Mapper 接口
* </p>
*
* @author car
* @since 2026-01-29
*/
public interface ShopOrderStatusMapper extends BaseMapper<ShopOrderStatus> {

    List<ShopOrderStatusVO> selectOrderStatusList(@Param("query") ShopOrderStatusDTO.ListQuery query);
}