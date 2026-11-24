package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaIgnore;
import com.sz.admin.shop_system.pojo.dto.ShopHomeNewsDTO;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewsVO;
import com.sz.admin.shop_system.service.ShopHomeNewsService;
import com.sz.core.common.entity.ApiPageResult;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户端首页资讯 Controller
 *
 * @author car
 * @since 2026-03-28
 */
@Tag(name = "[客户端] 首页资讯")
@RestController
@RequestMapping("/client/shopHomeNews")
@RequiredArgsConstructor
public class ClientHomeNewsController {

    private final ShopHomeNewsService lbHomeNewsService;

    @SaIgnore
    @Operation(summary = "首页资讯列表")
    @GetMapping("/index")
    public ApiResult<List<ShopHomeNewsVO>> frontList(@RequestParam(value = "size", defaultValue = "3") Integer size) {
        return ApiResult.success(lbHomeNewsService.frontList(size));
    }

    @SaIgnore
    @Operation(summary = "资讯频道分页")
    @GetMapping("/page")
    public ApiResult<PageResult<ShopHomeNewsVO>> frontPage(ShopHomeNewsDTO.FrontPage dto) {
        return ApiPageResult.success(lbHomeNewsService.frontPage(dto));
    }

    @SaIgnore
    @Operation(summary = "热门资讯")
    @GetMapping("/hot")
    public ApiResult<List<ShopHomeNewsVO>> hotList(@RequestParam(value = "size", defaultValue = "5") Integer size) {
        return ApiResult.success(lbHomeNewsService.hotList(size));
    }

    @SaIgnore
    @Operation(summary = "资讯详情")
    @GetMapping("/{id}")
    public ApiResult<ShopHomeNewsVO> frontDetail(@PathVariable Object id) {
        return ApiResult.success(lbHomeNewsService.frontDetail(id));
    }
}
