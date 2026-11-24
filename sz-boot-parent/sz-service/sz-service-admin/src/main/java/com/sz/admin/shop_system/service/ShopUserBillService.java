package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopUserBillDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopUserBill;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserBalanceSummaryVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserBillVO;
import com.sz.admin.shop_system.pojo.vo.ShopUserBillVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>
 * 用户帐单表 Service
 * </p>
 *
 * @author sz-admin
 * @since 2025-11-26
 */
public interface ShopUserBillService extends IService<ShopUserBill> {

    void create(ShopUserBillDTO.Create dto);

    void update(ShopUserBillDTO.Update dto);

    PageResult<ShopUserBillVO> page(ShopUserBillDTO.ListQuery dto);

    List<ShopUserBillVO> list(ShopUserBillDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopUserBillVO detail(Object id);

    void exportExcel(ShopUserBillDTO.ListQuery dto, HttpServletResponse response);

    ShopFrontUserBalanceSummaryVO frontSummary(Long userId);

    PageResult<ShopFrontUserBillVO> frontPage(Long userId, ShopUserBillDTO.FrontList dto);
}
