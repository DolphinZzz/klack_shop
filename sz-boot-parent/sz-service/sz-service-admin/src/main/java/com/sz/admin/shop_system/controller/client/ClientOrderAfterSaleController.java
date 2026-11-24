package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sz.admin.shop_system.pojo.dto.ShopOrderAfterSaleDTO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderAfterSaleVO;
import com.sz.admin.shop_system.service.ShopOrderAfterSaleService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.security.util.StpUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端订单售后 Controller
 */
@Tag(name = "[客户端] 订单售后")
@RestController
@RequestMapping("/client/shopOrderAfterSale")
@RequiredArgsConstructor
public class ClientOrderAfterSaleController {

    private final ShopOrderAfterSaleService lbOrderAfterSaleService;

    @Operation(summary = "PC端申请售后")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/apply")
    public ApiResult<Void> frontApply(@RequestBody ShopOrderAfterSaleDTO.FrontApply dto) {
        lbOrderAfterSaleService.frontApply(StpUserUtil.getLoginIdAsLong(), dto);
        return ApiResult.success();
    }

    @Operation(summary = "PC端售后列表")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/page")
    public ApiResult<PageResult<ShopFrontOrderAfterSaleVO>> frontPage(ShopOrderAfterSaleDTO.FrontList dto) {
        return ApiPageResult.success(lbOrderAfterSaleService.frontPage(StpUserUtil.getLoginIdAsLong(), dto));
    }

    @Operation(summary = "PC端售后详情")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/detail/{id}")
    public ApiResult<ShopFrontOrderAfterSaleVO> frontDetail(@PathVariable Long id) {
        return ApiResult.success(lbOrderAfterSaleService.frontDetail(StpUserUtil.getLoginIdAsLong(), id));
    }

    @Operation(summary = "PC端填写退货物流")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/return")
    public ApiResult<Void> frontSubmitReturn(@RequestBody ShopOrderAfterSaleDTO.FrontReturn dto) {
        lbOrderAfterSaleService.frontSubmitReturn(StpUserUtil.getLoginIdAsLong(), dto);
        return ApiResult.success();
    }
}
