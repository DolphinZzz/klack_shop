package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopUserRechargeActivityDTO;
import com.sz.admin.shop_system.pojo.vo.ShopUserRechargeActivityVO;
import com.sz.admin.shop_system.service.ShopUserRechargeActivityService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端充值活动表 Controller
 *
 * @author car
 * @since 2025-11-26
 */
@Tag(name = "[管理端] 充值活动表")
@RestController
@RequestMapping("/server/shopUserRechargeActivity")
@RequiredArgsConstructor
public class ServerUserRechargeActivityController {

    private final ShopUserRechargeActivityService shopUserRechargeActivityService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "shop.user.recharge.activity.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopUserRechargeActivityDTO.Create dto) {
        shopUserRechargeActivityService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "shop.user.recharge.activity.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopUserRechargeActivityDTO.Update dto) {
        shopUserRechargeActivityService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "shop.user.recharge.activity.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        shopUserRechargeActivityService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "shop.user.recharge.activity.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopUserRechargeActivityVO>> list(ShopUserRechargeActivityDTO.ListQuery dto) {
        return ApiPageResult.success(shopUserRechargeActivityService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "shop.user.recharge.activity.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopUserRechargeActivityVO> detail(@PathVariable Object id) {
        return ApiResult.success(shopUserRechargeActivityService.detail(id));
    }
}
