package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopUserAddressDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopUserAddress;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopUserAddressVO;

/**
 * <p>
 * 用户地址表 Service
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
public interface ShopUserAddressService extends IService<ShopUserAddress> {

    void create(ShopUserAddressDTO.Create dto);

    void update(ShopUserAddressDTO.Update dto);

    PageResult<ShopUserAddressVO> page(ShopUserAddressDTO.ListQuery dto);

    List<ShopUserAddressVO> list(ShopUserAddressDTO.ListQuery dto);

    void remove(SelectIdsDTO dto, Long uid);

    ShopUserAddressVO detail(Object id);
}
