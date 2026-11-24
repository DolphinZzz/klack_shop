package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopHomeNewsDTO;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewsVO;
import com.sz.admin.shop_system.service.ShopHomeNewsService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端首页资讯 Controller
 *
 * @author car
 * @since 2026-03-28
 */
@Tag(name = "[管理端] 首页资讯管理")
@RestController
@RequestMapping("/server/shopHomeNews")
@RequiredArgsConstructor
public class ServerHomeNewsController {

    private final ShopHomeNewsService shopHomeNewsService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "shop.home.news.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopHomeNewsDTO.Create dto) {
        shopHomeNewsService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "shop.home.news.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopHomeNewsDTO.Update dto) {
        shopHomeNewsService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "shop.home.news.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        shopHomeNewsService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "shop.home.news.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopHomeNewsVO>> list(ShopHomeNewsDTO.ListQuery dto) {
        return ApiPageResult.success(shopHomeNewsService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "shop.home.news.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopHomeNewsVO> detail(@PathVariable Object id) {
        return ApiResult.success(shopHomeNewsService.detail(id));
    }
}
