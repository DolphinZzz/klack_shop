package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopUserBillDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopUserIntegralBill;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserIntegralBillVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserIntegralSummaryVO;
import com.sz.core.common.entity.PageResult;

public interface ShopUserIntegralBillService extends IService<ShopUserIntegralBill> {

    PageResult<ShopFrontUserIntegralBillVO> frontPage(Long userId, ShopUserBillDTO.FrontIntegralList dto);

    ShopFrontUserIntegralSummaryVO frontSummary(Long userId);
}
