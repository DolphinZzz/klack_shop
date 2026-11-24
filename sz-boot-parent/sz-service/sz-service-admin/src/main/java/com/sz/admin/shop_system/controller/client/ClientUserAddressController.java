package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sz.admin.shop_system.pojo.dto.ShopUserAddressDTO;
import com.sz.admin.shop_system.pojo.vo.ShopUserAddressVO;
import com.sz.admin.shop_system.service.ShopUserAddressService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.security.util.StpUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户端用户收货地址 Controller
 *
 * @author car
 * @since 2025-11-26
 */
@Tag(name = "[客户端] 用户地址")
@RestController
@RequestMapping("/client/shopUserAddress")
@RequiredArgsConstructor
public class ClientUserAddressController {

    private final ShopUserAddressService shopUserAddressService;

    @Operation(summary = "新增用户地址-用户端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/address/add")
    public ApiResult<Void> create(@RequestBody ShopUserAddressDTO.Create dto) {
        dto.setUid(StpUserUtil.getLoginIdAsLong());
        shopUserAddressService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改用户地址-用户端")
    @SaCheckLogin(type = "shop_user")
    @PutMapping("/address/edit")
    public ApiResult<Void> update(@RequestBody ShopUserAddressDTO.Update dto) {
        dto.setUid(StpUserUtil.getLoginIdAsLong());
        shopUserAddressService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除用户地址-用户端")
    @SaCheckLogin(type = "shop_user")
    @DeleteMapping("/address/del")
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        shopUserAddressService.remove(dto, StpUserUtil.getLoginIdAsLong());
        return ApiResult.success();
    }

    @Operation(summary = "列表查询用户地址-用户端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/address/list")
    public ApiResult<PageResult<ShopUserAddressVO>> list(ShopUserAddressDTO.ListQuery dto) {
        dto.setUid(StpUserUtil.getLoginIdAsLong());
        return ApiPageResult.success(shopUserAddressService.page(dto));
    }

    @Operation(summary = "地址详情-用户端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/{id}")
    public ApiResult<ShopUserAddressVO> detail(@PathVariable Object id) {
        return ApiResult.success(shopUserAddressService.detail(id));
    }
}
