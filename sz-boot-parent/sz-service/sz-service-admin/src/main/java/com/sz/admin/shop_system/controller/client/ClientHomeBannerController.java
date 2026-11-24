package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaIgnore;
import com.sz.admin.shop_system.pojo.vo.ShopHomeBannerVO;
import com.sz.admin.shop_system.service.ShopHomeBannerService;
import com.sz.core.common.entity.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户端首页轮播 Controller
 *
 * @author car
 * @since 2026-02-26
 */
@Tag(name = "[客户端] 首页轮播")
@RestController
@RequestMapping("/client/shopHomeBanner")
@RequiredArgsConstructor
public class ClientHomeBannerController {

    private final ShopHomeBannerService shopHomeBannerService;

    @SaIgnore
    @Operation(summary = "首页轮播列表")
    @GetMapping("/index")
    public ApiResult<List<ShopHomeBannerVO>> frontList(@RequestParam(value = "size", defaultValue = "5") Integer size) {
        return ApiResult.success(shopHomeBannerService.frontList(size));
    }
}
