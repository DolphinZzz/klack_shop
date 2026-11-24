package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopCategoryDTO;
import com.sz.admin.shop_system.pojo.vo.ShopCategoryVO;
import com.sz.admin.shop_system.service.ShopCategoryService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端分类管理 Controller
 *
 * @author car
 * @since 2025-11-26
 */
@Tag(name = "[管理端] 分类管理")
@RestController
@RequestMapping("/server/shopCategory")
@RequiredArgsConstructor
public class ServerCategoryController {

    private final ShopCategoryService lbCategoryService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "lb.category.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopCategoryDTO.Create dto) {
        lbCategoryService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "lb.category.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopCategoryDTO.Update dto) {
        lbCategoryService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "lb.category.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        lbCategoryService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "lb.category.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopCategoryVO>> list(ShopCategoryDTO.ListQuery dto) {
        return ApiPageResult.success(lbCategoryService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "lb.category.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopCategoryVO> detail(@PathVariable Object id) {
        return ApiResult.success(lbCategoryService.detail(id));
    }

    @Operation(summary = "查询所有分类数据")
    @GetMapping("/allCate")
    public ApiResult<List<ShopCategoryVO>> categoryChildrenList() {
        return ApiResult.success(lbCategoryService.cateChildrenList());
    }
}
