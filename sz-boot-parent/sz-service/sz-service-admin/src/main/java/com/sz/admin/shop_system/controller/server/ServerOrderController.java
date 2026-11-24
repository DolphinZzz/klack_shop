package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopOrderDTO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderAdminDetailVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderVO;
import com.sz.admin.shop_system.service.ShopOrderService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端订单 Controller
 *
 * @author car
 * @since 2026-01-28
 */
@Tag(name = "[管理端] 订单管理")
@RestController
@RequestMapping("/server/shopOrder")
@RequiredArgsConstructor
public class ServerOrderController {

    private final ShopOrderService shopOrderService;

    @Operation(summary = "修改")
    @SaCheckPermission(value = "shop.order.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopOrderDTO.Update dto) {
        shopOrderService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "shop.order.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        shopOrderService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "shop.order.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopOrderVO>> list(ShopOrderDTO.ListQuery dto) {
        return ApiPageResult.success(shopOrderService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "shop.order.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopOrderVO> detail(@PathVariable Object id) {
        return ApiResult.success(shopOrderService.detail(id));
    }

    @Operation(summary = "根据订单号查询订单详情-管理端")
    @SaCheckPermission(value = "shop.order.query_table")
    @GetMapping("/detail/{orderNo}")
    public ApiResult<ShopOrderAdminDetailVO> detail(@PathVariable String orderNo) {
        return ApiResult.success(shopOrderService.adminDetail(orderNo));
    }

    @Operation(summary = "订单发货-管理端")
    @SaCheckPermission(value = "shop.order.update")
    @PostMapping("/deliver")
    public ApiResult<Void> deliver(@RequestBody ShopOrderDTO.Deliver dto) {
        shopOrderService.deliver(dto);
        return ApiResult.success();
    }
}
