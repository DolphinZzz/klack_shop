package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopUserDTO;
import com.sz.admin.shop_system.pojo.vo.ShopUserListVO;
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

    private final ShopUserService shopUserService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "shop.user.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopUserDTO.Create dto) {
        shopUserService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "shop.user.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopUserDTO.Update dto) {
        shopUserService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "shop.user.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        shopUserService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "shop.user.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopUserListVO>> list(ShopUserDTO.ListQuery dto) {
        return ApiPageResult.success(shopUserService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "shop.user.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopUserVO> detail(@PathVariable Object id) {
        return ApiResult.success(shopUserService.detail(id));
    }

    @Operation(summary = "导入")
    @Parameters({
            @Parameter(name = "file", description = "上传文件", schema = @Schema(type = "string", format = "binary"), required = true),
    })
    @SaCheckPermission(value = "shop.user.import")
    @PostMapping("/import")
    public void importExcel(@ModelAttribute ImportExcelDTO dto) {
        shopUserService.importExcel(dto);
    }

    @Operation(summary = "导出")
    @SaCheckPermission(value = "shop.user.export")
    @PostMapping("/export")
    public void exportExcel(@RequestBody ShopUserDTO.ListQuery dto, HttpServletResponse response) {
        shopUserService.exportExcel(dto, response);
    }
}
