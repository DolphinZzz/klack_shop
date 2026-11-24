package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopUserRechargeDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopUserRecharge;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopUserRechargeVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>
 * 用户充值表 Service
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
public interface ShopUserRechargeService extends IService<ShopUserRecharge> {

    void create(ShopUserRechargeDTO.Create dto);

    void frontRecharge(ShopUserRechargeDTO.Create dto);

    void update(ShopUserRechargeDTO.Update dto);

    PageResult<ShopUserRechargeVO> page(ShopUserRechargeDTO.ListQuery dto);

    List<ShopUserRechargeVO> list(ShopUserRechargeDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopUserRechargeVO detail(Object id);

    void exportExcel(ShopUserRechargeDTO.ListQuery dto, HttpServletResponse response);
}
