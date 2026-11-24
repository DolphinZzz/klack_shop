package com.sz.admin.shop_system.controller.server;

import com.sz.admin.shop_system.pojo.vo.ShopUserAddressVO;
import com.sz.admin.shop_system.service.ShopUserAddressService;
import com.sz.core.common.entity.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户地址表 Controller
 *
 * @author car
 * @since 2025-11-26
 */
@Tag(name = "[管理端] 用户地址表")
@RestController
@RequestMapping("/server/shopUserAddress")
@RequiredArgsConstructor
public class ServerUserAddressController {

    private final ShopUserAddressService shopUserAddressService;

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public ApiResult<ShopUserAddressVO> detail(@PathVariable Object id) {
        return ApiResult.success(shopUserAddressService.detail(id));
    }
}
