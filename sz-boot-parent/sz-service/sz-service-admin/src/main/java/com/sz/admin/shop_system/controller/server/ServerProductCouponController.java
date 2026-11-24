package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopProductCouponDTO;
import com.sz.admin.shop_system.pojo.vo.ShopCouponAndProductsVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductCouponVO;
import com.sz.admin.shop_system.service.ShopProductCouponService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端商品优惠券表 Controller
 *
 * @author car
 * @since 2026-01-30
 */
@Tag(name = "[管理端] 商品优惠券表")
@RestController
@RequestMapping("/server/shopProductCoupon")
@RequiredArgsConstructor
public class ServerProductCouponController {

    private final ShopProductCouponService shopProductCouponService;

    @Operation(summary = "新增")
    @SaCheckPermission(value = "shop.product.coupon.create")
    @PostMapping
    public ApiResult<Void> create(@RequestBody ShopProductCouponDTO.Create dto) {
        shopProductCouponService.create(dto);
        return ApiResult.success();
    }

    @Operation(summary = "修改")
    @SaCheckPermission(value = "shop.product.coupon.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopProductCouponDTO.Update dto) {
        shopProductCouponService.update(dto);
        return ApiResult.success();
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = "shop.product.coupon.remove")
    @DeleteMapping
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        shopProductCouponService.remove(dto);
        return ApiResult.success();
    }

    @Operation(summary = "列表查询")
    @SaCheckPermission(value = "shop.product.coupon.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopCouponAndProductsVO>> list(ShopProductCouponDTO.ListQuery dto) {
        return ApiPageResult.success(shopProductCouponService.page(dto));
    }

    @Operation(summary = "详情")
    @SaCheckPermission(value = "shop.product.coupon.query_table")
    @GetMapping("/{id}")
    public ApiResult<ShopProductCouponVO> detail(@PathVariable Object id) {
        return ApiResult.success(shopProductCouponService.detail(id));
    }
}
