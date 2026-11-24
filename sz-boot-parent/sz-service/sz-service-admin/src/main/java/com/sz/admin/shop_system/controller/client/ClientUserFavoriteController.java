package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sz.admin.shop_system.pojo.dto.ShopUserFavoriteDTO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserFavoriteStatusVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserFavoriteVO;
import com.sz.admin.shop_system.service.ShopUserFavoriteService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.security.util.StpUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端商品收藏 Controller
 */
@Tag(name = "[客户端] 商品收藏")
@RestController
@RequestMapping("/client/shopUserFavorite")
@RequiredArgsConstructor
public class ClientUserFavoriteController {

    private final ShopUserFavoriteService shopUserFavoriteService;

    @Operation(summary = "收藏商品-PC端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/create")
    public ApiResult<Void> create(@RequestParam("productId") Long productId) {
        shopUserFavoriteService.create(StpUserUtil.getLoginIdAsLong(), productId);
        return ApiResult.success();
    }

    @Operation(summary = "收藏列表查询-PC端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/page")
    public ApiResult<PageResult<ShopFrontUserFavoriteVO>> page(ShopUserFavoriteDTO.FrontList dto) {
        return ApiPageResult.success(shopUserFavoriteService.page(StpUserUtil.getLoginIdAsLong(), dto));
    }

    @Operation(summary = "批量取消收藏-PC端")
    @SaCheckLogin(type = "shop_user")
    @DeleteMapping("/delete")
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        shopUserFavoriteService.remove(dto, StpUserUtil.getLoginIdAsLong());
        return ApiResult.success();
    }

    @Operation(summary = "按商品取消收藏-PC端")
    @SaCheckLogin(type = "shop_user")
    @DeleteMapping("/product/{productId}")
    public ApiResult<Void> removeByProduct(@PathVariable("productId") Long productId) {
        shopUserFavoriteService.removeByProductId(StpUserUtil.getLoginIdAsLong(), productId);
        return ApiResult.success();
    }

    @Operation(summary = "查询商品收藏状态-PC端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/status")
    public ApiResult<ShopFrontUserFavoriteStatusVO> status(@RequestParam("productId") Long productId) {
        return ApiResult.success(shopUserFavoriteService.status(StpUserUtil.getLoginIdAsLong(), productId));
    }

    @Operation(summary = "收藏数量-PC端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/count")
    public ApiResult<Integer> count() {
        return ApiResult.success(shopUserFavoriteService.count(StpUserUtil.getLoginIdAsLong()));
    }
}
