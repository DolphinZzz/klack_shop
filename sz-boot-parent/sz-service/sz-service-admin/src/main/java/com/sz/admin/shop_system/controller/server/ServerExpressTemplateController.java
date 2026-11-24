package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopExpressTemplateDTO;
import com.sz.admin.shop_system.pojo.vo.ShopExpressTemplateVO;
import com.sz.admin.shop_system.service.ShopExpressTemplateService;
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
 * 管理端快递模板表 Controller
 *
 * @author car
 * @since 2026-01-22
 */
@Tag(name = "[管理端] 快递模板表")
@RestController
@RequestMapping("/server/shopExpressTemplate")
@RequiredArgsConstructor
public class ServerExpressTemplateController {

    private final ShopExpressTemplateService lbExpressTemplateService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "lb.express.template.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopExpressTemplateDTO.Create dto) {
        lbExpressTemplateService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "lb.express.template.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopExpressTemplateDTO.Update dto) {
        lbExpressTemplateService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "lb.express.template.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        lbExpressTemplateService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "lb.express.template.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopExpressTemplateVO>> list(ShopExpressTemplateDTO.ListQuery dto) {
        return ApiPageResult.success(lbExpressTemplateService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "lb.express.template.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopExpressTemplateVO> detail(@PathVariable Object id) {
        return ApiResult.success(lbExpressTemplateService.detail(id));
    }

    @Operation(summary = "导出")
    @SaCheckPermission(value = "lb.express.template.export")
    @PostMapping("/export")
    public void exportExcel(@RequestBody ShopExpressTemplateDTO.ListQuery dto, HttpServletResponse response) {
        lbExpressTemplateService.exportExcel(dto, response);
    }
}
