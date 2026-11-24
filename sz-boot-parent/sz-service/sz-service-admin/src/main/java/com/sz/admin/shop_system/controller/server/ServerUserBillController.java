package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopUserBillDTO;
import com.sz.admin.shop_system.pojo.vo.ShopUserBillVO;
import com.sz.admin.shop_system.service.ShopUserBillService;
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
 * 管理端用户帐单表 Controller
 *
 * @author sz-admin
 * @since 2025-11-26
 */
@Tag(name = "[管理端] 用户帐单表")
@RestController
@RequestMapping("/server/shopUserBill")
@RequiredArgsConstructor
public class ServerUserBillController {

    private final ShopUserBillService lbUserBillService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "lb.user.bill.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopUserBillDTO.Create dto) {
        lbUserBillService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "lb.user.bill.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopUserBillDTO.Update dto) {
        lbUserBillService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "lb.user.bill.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        lbUserBillService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "lb.user.bill.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopUserBillVO>> list(ShopUserBillDTO.ListQuery dto) {
        return ApiPageResult.success(lbUserBillService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "lb.user.bill.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopUserBillVO> detail(@PathVariable Object id) {
        return ApiResult.success(lbUserBillService.detail(id));
    }

    @Operation(summary = "导出")
    @SaCheckPermission(value = "lb.user.bill.export")
    @PostMapping("/export")
    public void exportExcel(@RequestBody ShopUserBillDTO.ListQuery dto, HttpServletResponse response) {
        lbUserBillService.exportExcel(dto, response);
    }
}
