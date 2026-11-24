package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sz.admin.shop_system.pojo.dto.ShopUserRechargeDTO;
import com.sz.admin.shop_system.service.ShopUserRechargeService;
import com.sz.core.common.entity.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端用户充值 Controller
 *
 * @author car
 * @since 2025-11-26
 */
@Tag(name = "[客户端] 用户充值")
@RestController
@RequestMapping("/client/shopUserRecharge")
@RequiredArgsConstructor
public class ClientUserRechargeController {

    private final ShopUserRechargeService shopUserRechargeService;

    @Operation(summary = "用户充值金额-客户端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/recharge")
    public ApiResult<Void> recharge(@RequestBody ShopUserRechargeDTO.Create dto) {
        shopUserRechargeService.frontRecharge(dto);
        return ApiResult.success();
    }
}
