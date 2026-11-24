package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sz.admin.shop_system.pojo.dto.ShopProductCouponDTO;
import com.sz.admin.shop_system.pojo.dto.ShopUserBillDTO;
import com.sz.admin.shop_system.pojo.vo.ShopCouponAndUserVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserBalanceSummaryVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserBillVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserIntegralBillVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserIntegralSummaryVO;
import com.sz.admin.shop_system.service.ShopProductCouponUserService;
import com.sz.admin.shop_system.service.ShopUserBillService;
import com.sz.admin.shop_system.service.ShopUserIntegralBillService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import com.sz.security.util.StpUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端资产中心 Controller
 */
@Tag(name = "[客户端] 资产中心")
@RestController
@RequestMapping("/client/shopUser")
@RequiredArgsConstructor
public class ClientUserAssetController {

    private final ShopUserBillService lbUserBillService;
    private final ShopProductCouponUserService lbProductCouponUserService;
    private final ShopUserIntegralBillService lbUserIntegralBillService;

    @Operation(summary = "我的余额汇总-PC端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/balance/summary")
    public ApiResult<ShopFrontUserBalanceSummaryVO> balanceSummary() {
        return ApiResult.success(lbUserBillService.frontSummary(StpUserUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "我的余额账单-PC端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/balance/bill")
    public ApiResult<PageResult<ShopFrontUserBillVO>> balanceBill(ShopUserBillDTO.FrontList dto) {
        return ApiPageResult.success(lbUserBillService.frontPage(StpUserUtil.getLoginIdAsLong(), dto));
    }

    @Operation(summary = "我的优惠券-PC端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/coupon/list")
    public ApiResult<PageResult<ShopCouponAndUserVO>> couponList(ShopProductCouponDTO.FrontUserList dto) {
        return ApiPageResult.success(lbProductCouponUserService.frontPage(StpUserUtil.getLoginIdAsLong(), dto));
    }

    @Operation(summary = "我的积分汇总-PC端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/integral/summary")
    public ApiResult<ShopFrontUserIntegralSummaryVO> integralSummary() {
        return ApiResult.success(lbUserIntegralBillService.frontSummary(StpUserUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "我的积分明细-PC端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/integral/bill")
    public ApiResult<PageResult<ShopFrontUserIntegralBillVO>> integralBill(ShopUserBillDTO.FrontIntegralList dto) {
        return ApiPageResult.success(lbUserIntegralBillService.frontPage(StpUserUtil.getLoginIdAsLong(), dto));
    }
}
