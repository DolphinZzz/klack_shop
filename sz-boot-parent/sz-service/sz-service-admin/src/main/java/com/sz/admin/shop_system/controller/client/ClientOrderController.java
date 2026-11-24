package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sz.admin.shop_system.pojo.dto.ShopFlashSaleDTO;
import com.sz.admin.shop_system.pojo.dto.ShopOrderDTO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderDetailVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontProductCommentVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderCreateVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderPaymentInfoVO;
import com.sz.admin.shop_system.pojo.vo.ShopPaymentVO;
import com.sz.admin.shop_system.pojo.vo.ShopPreOrderVO;
import com.sz.admin.shop_system.service.ShopOrderService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户端订单 Controller
 *
 * @author car
 * @since 2026-01-28
 */
@Tag(name = "[客户端] 订单中心")
@RestController
@RequestMapping("/client/shopOrder")
@RequiredArgsConstructor
public class ClientOrderController {

    private final ShopOrderService shopOrderService;

    @Operation(summary = "预下单-用户端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/pre/order")
    public ApiResult<ShopPreOrderVO> create(@RequestBody ShopOrderDTO.PreOrder dto) {
        return ApiResult.success(shopOrderService.create(dto));
    }

    @Operation(summary = "秒杀抢购预下单-用户端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/flash/reserve")
    public ApiResult<ShopOrderCreateVO> reserveFlashSale(@RequestBody ShopFlashSaleDTO.Reserve dto) {
        return ApiResult.success(shopOrderService.reserveFlashSale(dto));
    }

    @Operation(summary = "根据订单号查询预下单详情-用户端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/pre/order/{orderNo}")
    public ApiResult<ShopPreOrderVO> queryOrderNo(@PathVariable String orderNo) {
        return ApiResult.success(shopOrderService.queryDetail(orderNo));
    }

    @Operation(summary = "预下单金额重算（优惠券/积分）-用户端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/pre/order/recalculate")
    public ApiResult<ShopPreOrderVO> recalculate(@RequestBody ShopOrderDTO.Recalculate dto) {
        return ApiResult.success(shopOrderService.recalculate(dto));
    }

    @Operation(summary = "用户确认下单-用户端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/order/create")
    public ApiResult<ShopOrderCreateVO> orderCreate(@RequestBody ShopOrderDTO.Create dto) {
        return ApiResult.success(shopOrderService.orderCreate(dto));
    }

    @Operation(summary = "查询订单支付信息-用户端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/order/payment/{orderNo}")
    public ApiResult<ShopOrderPaymentInfoVO> paymentInfo(@PathVariable String orderNo) {
        return ApiResult.success(shopOrderService.paymentInfo(orderNo));
    }

    @Operation(summary = "用户支付-用户端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/order/payment")
    public ApiResult<ShopPaymentVO> payment(@RequestBody ShopOrderDTO.Payment dto) {
        return ApiResult.success(shopOrderService.payment(dto));
    }

    @Operation(summary = "订单中心列表-PC端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/order/list")
    public ApiResult<PageResult<ShopFrontOrderVO>> frontList(ShopOrderDTO.FrontList dto) {
        return ApiPageResult.success(shopOrderService.frontPage(dto));
    }

    @Operation(summary = "订单详情-PC端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/order/detail/{orderNo}")
    public ApiResult<ShopFrontOrderDetailVO> frontDetail(@PathVariable String orderNo) {
        return ApiResult.success(shopOrderService.frontDetail(orderNo));
    }

    @Operation(summary = "我的评论列表-PC端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/comment/list")
    public ApiResult<PageResult<ShopFrontProductCommentVO>> frontCommentList(ShopOrderDTO.FrontCommentList dto) {
        return ApiPageResult.success(shopOrderService.frontCommentPage(dto));
    }

    @Operation(summary = "确认收货-PC端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/order/receipt/{orderNo}")
    public ApiResult<Void> frontConfirmReceipt(@PathVariable String orderNo) {
        shopOrderService.frontConfirmReceipt(orderNo);
        return ApiResult.success();
    }

    @Operation(summary = "订单评价-PC端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/order/evaluate")
    public ApiResult<Void> frontEvaluate(@RequestBody ShopOrderDTO.FrontEvaluate dto) {
        shopOrderService.frontEvaluate(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除订单-PC端")
    @SaCheckLogin(type = "shop_user")
    @DeleteMapping("/order/{orderNo}")
    public ApiResult<Void> frontRemove(@PathVariable String orderNo) {
        shopOrderService.frontRemove(orderNo);
        return ApiResult.success();
    }
}
