package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaIgnore;
import com.sz.admin.shop_system.pojo.vo.ShopSystemCityVO;
import com.sz.admin.shop_system.service.ShopSystemCityService;
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
 * 客户端城市选择 Controller
 */
@Tag(name = "[客户端] 城市选择")
@RestController
@RequestMapping("/client/city")
@RequiredArgsConstructor
public class ClientSystemCityController {

    private final ShopSystemCityService lbSystemCityService;

    @SaIgnore
    @Operation(summary = "按父级和区域类型查询城市字典")
    @GetMapping("/list")
    public ApiResult<List<ShopSystemCityVO>> list(
            @RequestParam(required = false) Integer parentId,
            @RequestParam Integer regionType) {
        return ApiResult.success(lbSystemCityService.listByParentAndRegionType(parentId, regionType));
    }
}
