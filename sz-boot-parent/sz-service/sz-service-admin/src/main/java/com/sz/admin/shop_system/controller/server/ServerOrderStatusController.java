package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopOrderStatusDTO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderStatusVO;
import com.sz.admin.shop_system.service.ShopOrderStatusService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端订单状态表 Controller
 *
 * @author car
 * @since 2026-01-29
 */
@Tag(name = "[管理端] 订单状态表")
@RestController
@RequestMapping("/server/shopOrderStatus")
@RequiredArgsConstructor
public class ServerOrderStatusController {

    private final ShopOrderStatusService shopOrderStatusService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "shop.order.status.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopOrderStatusDTO.Create dto) {
        shopOrderStatusService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "shop.order.status.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopOrderStatusDTO.Update dto) {
        shopOrderStatusService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "shop.order.status.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        shopOrderStatusService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "shop.order.status.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopOrderStatusVO>> list(ShopOrderStatusDTO.ListQuery dto) {
        return ApiPageResult.success(shopOrderStatusService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "shop.order.status.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopOrderStatusVO> detail(@PathVariable Object id) {
        return ApiResult.success(shopOrderStatusService.detail(id));
    }
}
