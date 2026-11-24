package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sz.admin.shop_system.pojo.dto.ShopUserDTO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserProfileVO;
import com.sz.admin.shop_system.service.ShopUserService;
import com.sz.admin.system.service.SysFileService;
import com.sz.core.common.annotation.DebounceIgnore;
import com.sz.core.common.entity.ApiResult;
import com.sz.core.common.entity.UploadResult;
import com.sz.security.util.StpUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 客户端账户设置 Controller
 */
@Tag(name = "[客户端] 账户设置")
@RestController
@RequestMapping("/client/shopUser")
@RequiredArgsConstructor
public class ClientUserController {

    private final ShopUserService lbUserService;
    private final SysFileService sysFileService;

    @Operation(summary = "查询用户端个人资料")
    @SaCheckLogin(type = "shop_user")
    @GetMapping("/profile")
    public ApiResult<ShopFrontUserProfileVO> profile() {
        return ApiResult.success(lbUserService.getFrontProfile(StpUserUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "修改用户端个人资料")
    @SaCheckLogin(type = "shop_user")
    @PutMapping("/profile")
    public ApiResult<Void> updateProfile(@RequestBody ShopUserDTO.FrontProfileUpdate dto) {
        lbUserService.updateFrontProfile(StpUserUtil.getLoginIdAsLong(), dto);
        return ApiResult.success();
    }

    @Operation(summary = "上传用户端头像")
    @DebounceIgnore
    @SaCheckLogin(type = "shop_user")
    @PostMapping("/avatar/upload")
    public ApiResult<UploadResult> uploadAvatar(@RequestParam MultipartFile file,
                                                @RequestParam(value = "dirTag", defaultValue = "avatar") String dirTag) {
        return ApiResult.success(sysFileService.uploadFile(file, dirTag));
    }

    @Operation(summary = "修改用户端密码")
    @SaCheckLogin(type = "shop_user")
    @PutMapping("/password")
    public ApiResult<Void> updatePassword(@RequestBody ShopUserDTO.FrontPassword dto) {
        lbUserService.updateFrontPassword(StpUserUtil.getLoginIdAsLong(), dto);
        return ApiResult.success();
    }
}
