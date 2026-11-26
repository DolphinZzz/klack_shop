package com.sz.admin.shop_system.agent.service;

import com.sz.admin.shop_system.agent.pojo.dto.AgentMallDTO;
import com.sz.admin.shop_system.agent.pojo.vo.AgentMallVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderVO;
import com.sz.core.common.entity.PageResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * 智能客服 Agent 内网商城数据服务
 */
public interface AgentMallService {

    List<AgentMallVO.ProductSummary> recommendProducts(String sort, Integer categoryId, Integer limit);

    List<AgentMallVO.ProductSummary> searchProducts(String keyword, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, Integer limit);

    AgentMallVO.ProductDetail getProductDetail(Long productId);

    List<AgentMallVO.UserAddress> listUserAddresses(Long userId);

    PageResult<ShopFrontOrderVO> listUserOrders(Long userId, String tab, String keyword, Integer page, Integer limit);

    AgentMallVO.OrderDetail getOrderDetail(Long userId, String orderNo);

    List<AgentMallVO.AfterSaleSummary> listUserAfterSales(Long userId, String orderNo, String serviceNo, Integer limit);

    AgentMallVO.DirectOrderResult directCreateOrder(Long userId, AgentMallDTO.DirectCreateOrder dto);
}
