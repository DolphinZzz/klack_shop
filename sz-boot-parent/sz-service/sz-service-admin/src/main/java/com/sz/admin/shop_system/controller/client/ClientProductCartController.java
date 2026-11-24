package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sz.admin.shop_system.pojo.dto.ShopProductCartDTO;
import com.sz.admin.shop_system.pojo.vo.ShopProductCartVO;
import com.sz.admin.shop_system.service.ShopProductCartService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.security.util.StpUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户端购物车 Controller
 *
 * @author car
 * @since 2026-01-26
 */
@Tag(name = "[客户端] 购物车")
@RestController
@RequestMapping("/client/shopProductCart")
@RequiredArgsConstructor
public class ClientProductCartController {

    private final ShopProductCartService lbProductCartService;

    @Operation(summary = "新增购物车-用户端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/create")
    public ApiResult<Void> create(@RequestBody ShopProductCartDTO.Create dto) {
        long userId = StpUserUtil.getLoginIdAsLong();
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);
        lbProductCartService.create(dto, userId);
        return ApiResult.success();
    }

    @Operation(summary = "修改购物车-用户端")
    @SaCheckLogin(type = "shop_user")
    @PutMapping("/update")
    public ApiResult<Void> update(@RequestBody ShopProductCartDTO.Update dto) {
        long userId = StpUserUtil.getLoginIdAsLong();
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);
        lbProductCartService.update(dto, userId);
        return ApiResult.success();
    }

    @Operation(summary = "删除购物车-用户端")
    @SaCheckLogin(type = "shop_user")
    @DeleteMapping("/delete")
    public ApiResult<Void> remove(@RequestBody SelectIdsDTO dto) {
        long userId = StpUserUtil.getLoginIdAsLong();
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);
        lbProductCartService.remove(dto, userId);
        return ApiResult.success();
    }

    @Operation(summary = "购物车列表查询-用户端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/list")
    public ApiResult<PageResult<ShopProductCartVO>> list(ShopProductCartDTO.ListQuery dto) {
        return ApiPageResult.success(lbProductCartService.page(dto));
    }

    @Operation(summary = "购物车商品数量-用户端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/count")
    public ApiResult<Integer> count() {
        long userId = StpUserUtil.getLoginIdAsLong();
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);
        return ApiResult.success(lbProductCartService.count(userId));
    }
}
