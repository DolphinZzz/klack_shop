package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopFlashSaleDTO;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleDetailVO;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleVO;
import com.sz.admin.shop_system.service.ShopFlashSaleService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端秒杀营销管理 Controller
 */
@Tag(name = "[管理端] 秒杀营销管理")
@RestController
@RequestMapping("/server/shopFlashSale")
@RequiredArgsConstructor
public class ServerFlashSaleController {

    private final ShopFlashSaleService shopFlashSaleService;

    @Operation(summary = "营销管理列表")
    @SaCheckPermission(value = "shop.flash.sale.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopFlashSaleVO>> list(ShopFlashSaleDTO.ListQuery dto) {
        return ApiPageResult.success(shopFlashSaleService.page(dto));
    }

    @Operation(summary = "营销管理详情")
    @SaCheckPermission(value = "shop.flash.sale.query_table")
    @GetMapping("/detail/{productId}")
    public ApiResult<ShopFlashSaleDetailVO> detail(@PathVariable Long productId) {
        return ApiResult.success(shopFlashSaleService.detail(productId));
    }

    @Operation(summary = "营销管理更新")
    @SaCheckPermission(value = "shop.flash.sale.update")
    @PutMapping
    public ApiResult<Void> update(@RequestBody ShopFlashSaleDTO.Update dto) {
        shopFlashSaleService.updateMarketing(dto);
        return ApiResult.success();
    }
}
