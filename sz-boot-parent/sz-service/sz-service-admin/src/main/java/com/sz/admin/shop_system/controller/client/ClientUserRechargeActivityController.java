package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sz.admin.shop_system.pojo.vo.ShopUserRechargeActivityVO;
import com.sz.admin.shop_system.service.ShopUserRechargeActivityService;
import com.sz.core.common.entity.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户端充值活动 Controller
 *
 * @author car
 * @since 2025-11-26
 */
@Tag(name = "[客户端] 充值活动")
@RestController
@RequestMapping("/client/shopUserRechargeActivity")
@RequiredArgsConstructor
public class ClientUserRechargeActivityController {

    private final ShopUserRechargeActivityService shopUserRechargeActivityService;

    @Operation(summary = "充值活动列表-客户端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/list")
    public ApiResult<List<ShopUserRechargeActivityVO>> frontList() {
        return ApiResult.success(shopUserRechargeActivityService.frontList());
    }
}
