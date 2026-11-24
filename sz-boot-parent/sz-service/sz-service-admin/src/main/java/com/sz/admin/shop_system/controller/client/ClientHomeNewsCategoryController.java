package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaIgnore;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewsCategoryVO;
import com.sz.admin.shop_system.service.ShopHomeNewsCategoryService;
import com.sz.core.common.entity.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户端资讯分类 Controller
 *
 * @author car
 * @since 2026-03-28
 */
@Tag(name = "[客户端] 资讯分类")
@RestController
@RequestMapping("/client/shopHomeNewsCategory")
@RequiredArgsConstructor
public class ClientHomeNewsCategoryController {

    private final ShopHomeNewsCategoryService shopHomeNewsCategoryService;

    @SaIgnore
    @Operation(summary = "资讯频道分类")
    @GetMapping("/index")
    public ApiResult<List<ShopHomeNewsCategoryVO>> frontList() {
        return ApiResult.success(shopHomeNewsCategoryService.frontList());
    }
}
