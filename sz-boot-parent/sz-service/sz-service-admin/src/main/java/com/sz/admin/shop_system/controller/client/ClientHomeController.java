package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaIgnore;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleFrontVO;
import com.sz.admin.shop_system.pojo.vo.ShopHomeIndexVO;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewBookVO;
import com.sz.admin.shop_system.service.ShopHomeService;
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
 * 客户端首页聚合 Controller
 *
 * @author car
 * @since 2026-02-26
 */
@Tag(name = "[客户端] 首页聚合")
@RestController
@RequestMapping("/client/shopHome")
@RequiredArgsConstructor
public class ClientHomeController {

    private final ShopHomeService lbHomeService;

    @SaIgnore
    @Operation(summary = "PC端首页聚合数据")
    @GetMapping("/index")
    public ApiResult<ShopHomeIndexVO> frontIndex() {
        return ApiResult.success(lbHomeService.frontIndex());
    }

    @SaIgnore
    @Operation(summary = "首页新品推荐")
    @GetMapping("/recommend/new")
    public ApiResult<List<ShopHomeNewBookVO>> newRecommend(@RequestParam(value = "size", defaultValue = "8") Integer size) {
        return ApiResult.success(lbHomeService.newRecommend(size));
    }

    @SaIgnore
    @Operation(summary = "首页猜你喜欢")
    @GetMapping("/recommend/guess-like")
    public ApiResult<List<ShopHomeNewBookVO>> guessLike(@RequestParam(value = "size", defaultValue = "8") Integer size) {
        return ApiResult.success(lbHomeService.guessLike(size));
    }

    @SaIgnore
    @Operation(summary = "首页秒杀专区")
    @GetMapping("/flash")
    public ApiResult<List<ShopFlashSaleFrontVO>> flashSale(@RequestParam(value = "size", defaultValue = "4") Integer size) {
        return ApiResult.success(lbHomeService.flashSale(size));
    }
}
