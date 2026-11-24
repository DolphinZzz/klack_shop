package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopProductAttrDTO;
import com.sz.admin.shop_system.pojo.vo.ShopProductAttrVO;
import com.sz.admin.shop_system.service.ShopProductAttrService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端商品属性表 Controller
 *
 * @author car
 * @since 2025-12-04
 */
@Tag(name = "[管理端] 商品属性表")
@RestController
@RequestMapping("/server/shopProductAttr")
@RequiredArgsConstructor
public class ServerProductAttrController {

    private final ShopProductAttrService shopProductAttrService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "shop.product.attr.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopProductAttrDTO.Create dto) {
        shopProductAttrService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "shop.product.attr.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopProductAttrDTO.Update dto) {
        shopProductAttrService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "shop.product.attr.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        shopProductAttrService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "shop.product.attr.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopProductAttrVO>> list(ShopProductAttrDTO.ListQuery dto) {
        return ApiPageResult.success(shopProductAttrService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "shop.product.attr.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopProductAttrVO> detail(@PathVariable Object id) {
        return ApiResult.success(shopProductAttrService.detail(id));
    }
}
