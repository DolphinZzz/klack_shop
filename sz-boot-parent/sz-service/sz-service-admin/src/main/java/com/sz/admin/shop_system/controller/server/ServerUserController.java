package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopUserDTO;
import com.sz.admin.shop_system.pojo.vo.ShopUserVO;
import com.sz.admin.shop_system.service.ShopUserService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.ImportExcelDTO;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端用户表 Controller
 *
 * @author car
 * @since 2025-11-24
 */
@Tag(name = "[管理端] 用户表")
@RestController
@RequestMapping("/server/shopUser")
@RequiredArgsConstructor
public class ServerUserController {

    private final ShopUserService lbUserService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "lb.user.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopUserDTO.Create dto) {
        lbUserService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "lb.user.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopUserDTO.Update dto) {
        lbUserService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "lb.user.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        lbUserService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "lb.user.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopUserVO>> list(ShopUserDTO.ListQuery dto) {
        return ApiPageResult.success(lbUserService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "lb.user.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopUserVO> detail(@PathVariable Object id) {
        return ApiResult.success(lbUserService.detail(id));
    }

    @Operation(summary = "导入")
    @Parameters({
            @Parameter(name = "file", description = "上传文件", schema = @Schema(type = "string", format = "binary"), required = true),
    })
    @SaCheckPermission(value = "lb.user.import")
    @PostMapping("/import")
    public void importExcel(@ModelAttribute ImportExcelDTO dto) {
        lbUserService.importExcel(dto);
    }

    @Operation(summary = "导出")
    @SaCheckPermission(value = "lb.user.export")
    @PostMapping("/export")
    public void exportExcel(@RequestBody ShopUserDTO.ListQuery dto, HttpServletResponse response) {
        lbUserService.exportExcel(dto, response);
    }
}
