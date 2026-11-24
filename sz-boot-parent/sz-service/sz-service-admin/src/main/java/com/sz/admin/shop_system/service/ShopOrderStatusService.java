package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopOrderStatusDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopOrderStatus;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopOrderStatusVO;

/**
 * <p>
 * 订单状态表 Service
 * </p>
 *
 * @author car
 * @since 2026-01-29
 */
public interface ShopOrderStatusService extends IService<ShopOrderStatus> {

    void create(ShopOrderStatusDTO.Create dto);

    void update(ShopOrderStatusDTO.Update dto);

    PageResult<ShopOrderStatusVO> page(ShopOrderStatusDTO.ListQuery dto);

    List<ShopOrderStatusVO> list(ShopOrderStatusDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopOrderStatusVO detail(Object id);
}