package com.sz.admin.shop_system.agent.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.sz.admin.shop_system.agent.pojo.dto.AgentMallDTO;
import com.sz.admin.shop_system.agent.pojo.vo.AgentMallVO;
import com.sz.admin.shop_system.agent.service.AgentMallService;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderVO;
import com.sz.core.common.annotation.DebounceIgnore;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 智能客服 Agent 内网商城数据与直购建单 Controller（由 AgentInternalAuthInterceptor 校验 X-Internal-Secret）
 */
@Tag(name = "[内网] 智能客服商城接口")
@RestController
@RequestMapping("/internal/agent")
@RequiredArgsConstructor
public class AgentInternalController {

    private final AgentMallService agentMallService;

    @SaIgnore
    @Operation(summary = "内网推荐商品查询")
    @GetMapping("/products/recommend")
    public ApiResult<List<AgentMallVO.ProductSummary>> recommendProducts(
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "5") Integer limit) {
        return ApiResult.success(agentMallService.recommendProducts(sort, categoryId, limit));
    }

    @SaIgnore
    @Operation(summary = "内网商品搜索")
    @GetMapping("/products/search")
    public ApiResult<List<AgentMallVO.ProductSummary>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "6") Integer limit) {
        return ApiResult.success(agentMallService.searchProducts(keyword, categoryId, minPrice, maxPrice, limit));
    }

    @SaIgnore
    @Operation(summary = "内网商品详情与全量SKU查询")
    @GetMapping("/products/{productId}")
    public ApiResult<AgentMallVO.ProductDetail> getProductDetail(@PathVariable Long productId) {
        return ApiResult.success(agentMallService.getProductDetail(productId));
    }

    @SaIgnore
    @Operation(summary = "内网用户收货地址查询")
    @GetMapping("/users/{userId}/addresses")
    public ApiResult<List<AgentMallVO.UserAddress>> listUserAddresses(@PathVariable Long userId) {
        return ApiResult.success(agentMallService.listUserAddresses(userId));
    }

    @SaIgnore
    @Operation(summary = "内网用户订单列表查询")
    @GetMapping("/users/{userId}/orders")
    public ApiResult<PageResult<ShopFrontOrderVO>> listUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "all") String tab,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer limit) {
        return ApiPageResult.success(agentMallService.listUserOrders(userId, tab, keyword, page, limit));
    }

    @SaIgnore
    @Operation(summary = "内网用户订单详情与发货记录查询")
    @GetMapping("/users/{userId}/orders/{orderNo}")
    public ApiResult<AgentMallVO.OrderDetail> getOrderDetail(@PathVariable Long userId,
                                                             @PathVariable String orderNo) {
        return ApiResult.success(agentMallService.getOrderDetail(userId, orderNo));
    }

    @SaIgnore
    @Operation(summary = "内网用户售后单列表查询")
    @GetMapping("/users/{userId}/after-sales")
    public ApiResult<List<AgentMallVO.AfterSaleSummary>> listUserAfterSales(
            @PathVariable Long userId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String serviceNo,
            @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResult.success(agentMallService.listUserAfterSales(userId, orderNo, serviceNo, limit));
    }

    @SaIgnore
    @DebounceIgnore
    @Operation(summary = "内网聊天确认幂等直购建单")
    @PostMapping("/users/{userId}/orders/direct-create")
    public ApiResult<AgentMallVO.DirectOrderResult> directCreateOrder(@PathVariable Long userId,
                                                                      @RequestBody AgentMallDTO.DirectCreateOrder dto) {
        return ApiResult.success(agentMallService.directCreateOrder(userId, dto));
    }
}
