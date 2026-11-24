package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopHomeNewsCategoryDTO;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewsCategoryVO;
import com.sz.admin.shop_system.service.ShopHomeNewsCategoryService;
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
 * 管理端资讯分类 Controller
 *
 * @author car
 * @since 2026-03-28
 */
@Tag(name = "[管理端] 首页资讯分类")
@RestController
@RequestMapping("/server/shopHomeNewsCategory")
@RequiredArgsConstructor
public class ServerHomeNewsCategoryController {

    private final ShopHomeNewsCategoryService lbHomeNewsCategoryService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "lb.home.news.category.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopHomeNewsCategoryDTO.Create dto) {
        lbHomeNewsCategoryService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "lb.home.news.category.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopHomeNewsCategoryDTO.Update dto) {
        lbHomeNewsCategoryService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "lb.home.news.category.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        lbHomeNewsCategoryService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "lb.home.news.category.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopHomeNewsCategoryVO>> list(ShopHomeNewsCategoryDTO.ListQuery dto) {
        return ApiPageResult.success(lbHomeNewsCategoryService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "lb.home.news.category.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopHomeNewsCategoryVO> detail(@PathVariable Object id) {
        return ApiResult.success(lbHomeNewsCategoryService.detail(id));
    }

    @Operation(summary = "全部分类")
    @GetMapping("/all")
    public ApiResult<List<ShopHomeNewsCategoryVO>> allList() {
        return ApiResult.success(lbHomeNewsCategoryService.frontList());
    }
}
