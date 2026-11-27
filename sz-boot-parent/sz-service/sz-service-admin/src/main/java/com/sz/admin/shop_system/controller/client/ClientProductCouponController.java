package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sz.admin.shop_system.pojo.dto.ShopProductCouponDTO;
import com.sz.admin.shop_system.pojo.vo.ShopProductCouponVO;
import com.sz.admin.shop_system.service.ShopProductCouponService;
import com.sz.core.common.annotation.DebounceIgnore;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 客户端领券中心 Controller
 *
 * @author car
 * @since 2026-01-30
 */
@Slf4j
@Tag(name = "[客户端] 领券中心")
@RestController
@RequestMapping("/client/shopProductCoupon")
@RequiredArgsConstructor
public class ClientProductCouponController {

    private final ShopProductCouponService shopProductCouponService;

    @Operation(summary = "领券中心列表-用户端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/list")
    public ApiResult<PageResult<ShopProductCouponVO>> frontList(ShopProductCouponDTO.ListQuery dto) {
        return ApiPageResult.success(shopProductCouponService.frontList(dto));
    }

    @DebounceIgnore
    @Operation(summary = "抢优惠券-用户端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/grab/{couponId}")
    public ApiResult<String> grabCoupon(@PathVariable Long couponId) {
        String orderId = shopProductCouponService.grabCoupon(couponId);
        return ApiResult.success(orderId);
    }
}
