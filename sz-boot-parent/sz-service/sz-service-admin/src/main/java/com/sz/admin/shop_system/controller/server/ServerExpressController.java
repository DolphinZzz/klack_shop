package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopExpressDTO;
import com.sz.admin.shop_system.pojo.vo.ShopExpressVO;
import com.sz.admin.shop_system.service.ShopExpressService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端快递表 Controller
 *
 * @author car
 * @since 2026-01-22
 */
@Tag(name = "[管理端] 快递表")
@RestController
@RequestMapping("/server/shopExpress")
@RequiredArgsConstructor
public class ServerExpressController {

    private final ShopExpressService shopExpressService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "shop.express.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopExpressDTO.Create dto) {
        shopExpressService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "shop.express.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopExpressDTO.Update dto) {
        shopExpressService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "shop.express.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        shopExpressService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "shop.express.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopExpressVO>> list(ShopExpressDTO.ListQuery dto) {
        return ApiPageResult.success(shopExpressService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "shop.express.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopExpressVO> detail(@PathVariable Object id) {
        return ApiResult.success(shopExpressService.detail(id));
    }
}
