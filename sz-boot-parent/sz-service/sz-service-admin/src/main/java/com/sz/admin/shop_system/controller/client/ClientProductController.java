package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaIgnore;
import com.sz.admin.shop_system.pojo.dto.ShopProductDTO;
import com.sz.admin.shop_system.pojo.page.ProductDetailResponse;
import com.sz.admin.shop_system.pojo.page.ProductResponse;
import com.sz.admin.shop_system.pojo.vo.ShopFrontProductCommentVO;
import com.sz.admin.shop_system.service.ShopProductService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户端商品 Controller
 *
 * @author car
 * @since 2025-12-03
 */
@Tag(name = "[客户端] 商品查询")
@RestController
@RequestMapping("/client/shopProduct")
@RequiredArgsConstructor
public class ClientProductController {

    private final ShopProductService lbProductService;

    @SaIgnore
    @Operation(summary = "根据分类id-查询商品列表-用户端")
    @PostMapping("/list")
    public ApiResult<PageResult<ProductResponse>> getProductList(@RequestBody ShopProductDTO.FrontList request) {
        return ApiResult.success(lbProductService.getCategoryProductList(request));
    }

    @SaIgnore
    @Operation(summary = "查询商品详情-用户端")
    @GetMapping("/detail/{id}")
    public ApiResult<ProductDetailResponse> getDetail(@PathVariable Integer id,
                                                      @RequestParam(value = "type", defaultValue = "normal") String type) {
        return ApiResult.success(lbProductService.getDetail(id, type));
    }

    @SaIgnore
    @Operation(summary = "查询商品评论-用户端")
    @GetMapping("/comment/{productId}")
    public ApiResult<PageResult<ShopFrontProductCommentVO>> getFrontCommentPage(@PathVariable Integer productId,
                                                                              ShopProductDTO.FrontCommentPage dto) {
        return ApiPageResult.success(lbProductService.getFrontCommentPage(productId, dto));
    }
}
