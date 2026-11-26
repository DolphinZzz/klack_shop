package com.sz.admin.shop_system.service;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.agent.pojo.dto.AgentMallDTO;
import com.sz.admin.shop_system.agent.pojo.vo.AgentMallVO;
import com.sz.admin.shop_system.pojo.dto.*;
import com.sz.admin.shop_system.pojo.vo.ShopOrderCreateVO;
import com.sz.admin.shop_system.pojo.po.ShopOrder;
import com.sz.admin.shop_system.pojo.vo.ShopOrderAdminDetailVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderDetailVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontProductCommentVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderVO;
import com.sz.admin.shop_system.pojo.vo.ShopPaymentVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderPaymentInfoVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderVO;
import com.sz.admin.shop_system.pojo.vo.ShopPreOrderVO;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;

/**
 * <p>
 * 订单表 Service
 * </p>
 *
 * @author car
 * @since 2026-01-28
 */
public interface ShopOrderService extends IService<ShopOrder> {

    ShopPreOrderVO create(ShopOrderDTO.PreOrder dto);

    ShopOrderCreateVO reserveFlashSale(ShopFlashSaleDTO.Reserve dto);

    void update(ShopOrderDTO.Update dto);

    void remove(SelectIdsDTO dto);

    ShopPreOrderVO queryDetail(String orderNo);

    ShopPreOrderVO recalculate(ShopOrderDTO.Recalculate dto);

    ShopOrderCreateVO orderCreate(ShopOrderDTO.Create dto);

    ShopOrderPaymentInfoVO paymentInfo(String orderNo);

    ShopPaymentVO payment(ShopOrderDTO.Payment dto);

    PageResult<ShopFrontOrderVO> frontPage(ShopOrderDTO.FrontList dto);

    PageResult<ShopFrontOrderVO> frontPageForUser(Long userId, ShopOrderDTO.FrontList dto);

    ShopFrontOrderDetailVO frontDetail(String orderNo);

    ShopFrontOrderDetailVO frontDetailForUser(Long userId, String orderNo);

    AgentMallVO.DirectOrderResult directCreateForAgent(Long userId, AgentMallDTO.DirectCreateOrder dto);

    PageResult<ShopFrontProductCommentVO> frontCommentPage(ShopOrderDTO.FrontCommentList dto);

    void frontConfirmReceipt(String orderNo);

    void frontEvaluate(ShopOrderDTO.FrontEvaluate dto);

    void frontRemove(String orderNo);

    PageResult<ShopOrderVO> page(ShopOrderDTO.ListQuery dto);

    ShopOrderVO detail(Object id);

    ShopOrderAdminDetailVO adminDetail(String orderNo);

    void deliver(ShopOrderDTO.Deliver dto);
}
