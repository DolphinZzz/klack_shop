package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopProductDTO;
import com.sz.admin.shop_system.pojo.vo.ShopProductDetailVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductVO;
import com.sz.admin.shop_system.service.ShopProductService;
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
 * 管理端商品表 Controller
 *
 * @author car
 * @since 2025-12-03
 */
@Tag(name = "[管理端] 商品表")
@RestController
@RequestMapping("/server/shopProduct")
@RequiredArgsConstructor
public class ServerProductController {

    private final ShopProductService lbProductService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "lb.product.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopProductDTO.Create dto) {
        lbProductService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "lb.product.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopProductDTO.Update dto) {
        lbProductService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "lb.product.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        lbProductService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "lb.product.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopProductVO>> list(ShopProductDTO.ListQuery dto) {
        return ApiPageResult.success(lbProductService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "lb.product.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopProductDetailVO> detail(@PathVariable Object id) {
        return ApiResult.success(lbProductService.detail(id));
    }

    @Operation(summary = "导出")
    @SaCheckPermission(value = "lb.product.export")
    @PostMapping("/export")
    public void exportExcel(@RequestBody ShopProductDTO.ListQuery dto, HttpServletResponse response) {
        lbProductService.exportExcel(dto, response);
    }

    @Operation(summary = "后台管理-商品详情")
    @GetMapping("/detail/{productId}")
    public ApiResult<ShopProductDetailVO> getDetail(@PathVariable Integer productId) {
        return ApiResult.success(lbProductService.detail(productId));
    }
}
