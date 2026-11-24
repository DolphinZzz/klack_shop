package com.sz.admin.shop_system.controller.server;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sz.admin.shop_system.pojo.dto.ShopOrderAfterSaleDTO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderAfterSaleVO;
import com.sz.admin.shop_system.service.ShopOrderAfterSaleService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
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
 * 管理端订单售后 Controller
 */
@Tag(name = "[管理端] 订单售后")
@RestController
@RequestMapping("/server/shopOrderAfterSale")
@RequiredArgsConstructor
public class ServerOrderAfterSaleController {

    private final ShopOrderAfterSaleService lbOrderAfterSaleService;

    @Operation(summary = "管理端售后列表")
    @SaCheckPermission(value = "lb.order.after.sale.query_table")
    @GetMapping
    public ApiResult<PageResult<ShopOrderAfterSaleVO>> page(ShopOrderAfterSaleDTO.ListQuery dto) {
        return ApiPageResult.success(lbOrderAfterSaleService.page(dto));
    }

    @Operation(summary = "管理端售后详情")
    @SaCheckPermission(value = "lb.order.after.sale.query_table")
    @GetMapping("/detail/{id}")
    public ApiResult<ShopOrderAfterSaleVO> detail(@PathVariable Long id) {
        return ApiResult.success(lbOrderAfterSaleService.detail(id));
    }

    @Operation(summary = "管理端审核售后")
    @SaCheckPermission(value = "lb.order.after.sale.update")
    @PostMapping("/audit")
    public ApiResult<Void> audit(@RequestBody ShopOrderAfterSaleDTO.Audit dto) {
        lbOrderAfterSaleService.audit(dto);
        return ApiResult.success();
    }

    @Operation(summary = "管理端确认收货并退款")
    @SaCheckPermission(value = "lb.order.after.sale.update")
    @PostMapping("/receive")
    public ApiResult<Void> receive(@RequestBody ShopOrderAfterSaleDTO.Receive dto) {
        lbOrderAfterSaleService.receive(dto);
        return ApiResult.success();
    }
}
