package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaIgnore;
import com.sz.admin.shop_system.pojo.dto.ShopCategoryDTO;
import com.sz.admin.shop_system.pojo.vo.ShopCategoryVO;
import com.sz.admin.shop_system.service.ShopCategoryService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户端分类 Controller
 *
 * @author car
 * @since 2025-11-26
 */
@Tag(name = "[客户端] 分类查询")
@RestController
@RequestMapping("/client/shopCategory")
@RequiredArgsConstructor
public class ClientCategoryController {

    private final ShopCategoryService shopCategoryService;

    @SaIgnore
    @Operation(summary = "首页分类数据")
    @GetMapping("/index")
    public ApiResult<List<ShopCategoryVO>> frontCategoryList() {
        return ApiResult.success(shopCategoryService.cateChildrenList());
    }

    @SaIgnore
    @Operation(summary = "查询所有分类数据")
    @GetMapping("/allCate")
    public ApiResult<List<ShopCategoryVO>> categoryChildrenList() {
        return ApiResult.success(shopCategoryService.cateChildrenList());
    }

    @SaIgnore
    @Operation(summary = "分类列表分页查询")
    @GetMapping
    public ApiResult<PageResult<ShopCategoryVO>> list(ShopCategoryDTO.ListQuery dto) {
        return ApiPageResult.success(shopCategoryService.page(dto));
    }
}
