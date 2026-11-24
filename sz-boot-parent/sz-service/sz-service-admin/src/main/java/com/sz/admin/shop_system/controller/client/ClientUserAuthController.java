package com.sz.admin.shop_system.controller.client;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import com.sz.admin.shop_system.pojo.dto.ShopUserDTO;
import com.sz.admin.shop_system.pojo.vo.ShopUserLoginVO;
import com.sz.admin.shop_system.pojo.vo.ShopUserVO;
import com.sz.admin.shop_system.service.ShopUserService;
import com.sz.core.common.annotation.DebounceIgnore;
import com.sz.core.common.entity.ApiResult;
import com.sz.security.util.StpUserTokenRedisUtil;
import com.sz.security.util.StpUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 客户端用户认证 Controller
 *
 * @author car
 * @since 2025-01-27
 */
@Tag(name = "[客户端] 用户认证")
@RestController
@RequestMapping("/client/lb/auth")
@RequiredArgsConstructor
@Slf4j
public class ClientUserAuthController {

    private final ShopUserService lbUserService;

    @SaIgnore
    @DebounceIgnore
    @Operation(summary = "用户端注册")
    @PostMapping("/register")
    public ApiResult<Void> register(@Valid @RequestBody ShopUserDTO.Register dto) {
        lbUserService.userRegister(dto);
        return ApiResult.success();
    }

    @SaIgnore
    @DebounceIgnore
    @Operation(summary = "用户端登录")
    @PostMapping("/login")
    public ApiResult<ShopUserLoginVO> login(@Valid @RequestBody ShopUserDTO.Login dto) {
        ShopUserVO userVO = lbUserService.buildLoginUser(dto.getAccountOrPhone(), dto.getPassword());

        ShopUserLoginVO loginVO = new ShopUserLoginVO();
        loginVO.setUid(userVO.getUid());
        loginVO.setAccount(userVO.getAccount());
        loginVO.setNickname(userVO.getNickname());
        loginVO.setRealName(userVO.getRealName());
        loginVO.setPhone(userVO.getPhone());
        loginVO.setAvatar(userVO.getAvatar());
        loginVO.setNowMoney(userVO.getNowMoney());
        loginVO.setIntegral(userVO.getIntegral());
        loginVO.setExperience(userVO.getExperience());
        loginVO.setLevel(userVO.getLevel());
        loginVO.setUserType(userVO.getUserType());
        loginVO.setCreateTime(userVO.getCreateTime());

        // 使用SaToken进行登录
        StpUserUtil.login(userVO.getUid());

        // 设置token信息
        loginVO.setAccessToken(StpUserUtil.getTokenValue());
        loginVO.setExpireIn(StpUserUtil.getTokenTimeout());
        StpUserTokenRedisUtil.saveToken(userVO.getUid(), loginVO.getAccessToken(), loginVO.getExpireIn());

        return ApiResult.success(loginVO);
    }

    @SaCheckLogin(type = "shop_user")
    @Operation(summary = "用户端退出登出")
    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        String tokenValue = StpUserUtil.getTokenValue();
        StpUserTokenRedisUtil.removeToken(tokenValue);
        StpUserUtil.logout();
        return ApiResult.success();
    }
}
