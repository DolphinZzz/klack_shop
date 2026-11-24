package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaIgnore;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleFrontVO;
import com.sz.admin.shop_system.service.ShopFlashSaleService;
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
 * 客户端秒杀活动 Controller
 */
@Tag(name = "[客户端] 秒杀活动")
@RestController
@RequestMapping("/client/shopFlashSale")
@RequiredArgsConstructor
public class ClientFlashSaleController {

    private final ShopFlashSaleService shopFlashSaleService;

    @SaIgnore
    @Operation(summary = "前台秒杀列表")
    @GetMapping("/list")
    public ApiResult<List<ShopFlashSaleFrontVO>> frontList(@RequestParam(value = "size", defaultValue = "4") Integer size) {
        return ApiResult.success(shopFlashSaleService.activeFrontList(size));
    }

    @SaIgnore
    @Operation(summary = "前台秒杀详情")
    @GetMapping("/detail")
    public ApiResult<ShopFlashSaleFrontVO> frontDetail(@RequestParam("productId") Long productId) {
        return ApiResult.success(shopFlashSaleService.frontDetail(productId));
    }
}
