package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopOrderAfterSaleDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopOrderAfterSale;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderAfterSaleVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderAfterSaleVO;
import com.sz.core.common.entity.PageResult;

public interface ShopOrderAfterSaleService extends IService<ShopOrderAfterSale> {

    void frontApply(Long userId, ShopOrderAfterSaleDTO.FrontApply dto);

    PageResult<ShopFrontOrderAfterSaleVO> frontPage(Long userId, ShopOrderAfterSaleDTO.FrontList dto);

    ShopFrontOrderAfterSaleVO frontDetail(Long userId, Long id);

    void frontSubmitReturn(Long userId, ShopOrderAfterSaleDTO.FrontReturn dto);

    PageResult<ShopOrderAfterSaleVO> page(ShopOrderAfterSaleDTO.ListQuery dto);

    ShopOrderAfterSaleVO detail(Long id);

    void audit(ShopOrderAfterSaleDTO.Audit dto);

    void receive(ShopOrderAfterSaleDTO.Receive dto);
}