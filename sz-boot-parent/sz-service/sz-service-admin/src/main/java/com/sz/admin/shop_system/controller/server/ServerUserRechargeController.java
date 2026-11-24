package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopUserRechargeDTO;
import com.sz.admin.shop_system.pojo.vo.ShopUserRechargeVO;
import com.sz.admin.shop_system.service.ShopUserRechargeService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端用户充值表 Controller
 *
 * @author car
 * @since 2025-11-26
 */
@Tag(name = "[管理端] 用户充值表")
@RestController
@RequestMapping("/server/shopUserRecharge")
@RequiredArgsConstructor
public class ServerUserRechargeController {

    private final ShopUserRechargeService lbUserRechargeService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "lb.user.recharge.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopUserRechargeDTO.Create dto) {
        lbUserRechargeService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "lb.user.recharge.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopUserRechargeDTO.Update dto) {
        lbUserRechargeService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "lb.user.recharge.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        lbUserRechargeService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "lb.user.recharge.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopUserRechargeVO>> list(ShopUserRechargeDTO.ListQuery dto) {
        return ApiPageResult.success(lbUserRechargeService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "lb.user.recharge.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopUserRechargeVO> detail(@PathVariable Object id) {
        return ApiResult.success(lbUserRechargeService.detail(id));
    }

    @Operation(summary = "导出")
    @SaCheckPermission(value = "lb.user.recharge.export")
    @PostMapping("/export")
    public void exportExcel(@RequestBody ShopUserRechargeDTO.ListQuery dto, HttpServletResponse response) {
        lbUserRechargeService.exportExcel(dto, response);
    }
}
