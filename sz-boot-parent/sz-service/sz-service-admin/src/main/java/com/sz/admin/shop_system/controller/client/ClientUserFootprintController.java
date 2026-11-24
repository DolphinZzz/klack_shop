package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sz.admin.shop_system.pojo.dto.ShopUserFootprintDTO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserFootprintVO;
import com.sz.admin.shop_system.service.ShopUserFootprintService;
import com.sz.core.common.entity.ApiResult;
import com.sz.security.util.StpUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户端我的足迹 Controller
 */
@Tag(name = "[客户端] 我的足迹")
@RestController
@RequestMapping("/client/shopUserFootprint")
@RequiredArgsConstructor
public class ClientUserFootprintController {

    private final ShopUserFootprintService lbUserFootprintService;

    @Operation(summary = "记录商品浏览足迹-PC端")
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/record")
    public ApiResult<Void> record(@RequestParam("productId") Long productId) {
        lbUserFootprintService.record(StpUserUtil.getLoginIdAsLong(), productId);
        return ApiResult.success();
    }

    @Operation(summary = "获取我的足迹-PC端")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/list")
    public ApiResult<List<ShopFrontUserFootprintVO>> list(ShopUserFootprintDTO.FrontList dto) {
        return ApiResult.success(lbUserFootprintService.frontList(StpUserUtil.getLoginIdAsLong(), dto));
    }
}
