package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopExpressDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopExpress;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopExpressVO;

/**
 * <p>
 * 快递表 Service
 * </p>
 *
 * @author car
 * @since 2026-01-22
 */
public interface ShopExpressService extends IService<ShopExpress> {

    void create(ShopExpressDTO.Create dto);

    void update(ShopExpressDTO.Update dto);

    PageResult<ShopExpressVO> page(ShopExpressDTO.ListQuery dto);

    List<ShopExpressVO> list(ShopExpressDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopExpressVO detail(Object id);
}