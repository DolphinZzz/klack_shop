package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopHomeBannerDTO;
import com.sz.admin.shop_system.pojo.vo.ShopHomeBannerVO;
import com.sz.admin.shop_system.service.ShopHomeBannerService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端首页轮播 Controller
 *
 * @author car
 * @since 2026-02-26
 */
@Tag(name = "[管理端] 首页轮播配置")
@RestController
@RequestMapping("/server/shopHomeBanner")
@RequiredArgsConstructor
public class ServerHomeBannerController {

    private final ShopHomeBannerService lbHomeBannerService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "lb.home.banner.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopHomeBannerDTO.Create dto) {
        lbHomeBannerService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "lb.home.banner.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopHomeBannerDTO.Update dto) {
        lbHomeBannerService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "lb.home.banner.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        lbHomeBannerService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "lb.home.banner.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopHomeBannerVO>> list(ShopHomeBannerDTO.ListQuery dto) {
        return ApiPageResult.success(lbHomeBannerService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "lb.home.banner.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopHomeBannerVO> detail(@PathVariable Object id) {
        return ApiResult.success(lbHomeBannerService.detail(id));
    }
}
