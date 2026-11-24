package com.sz.admin.shop_system.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户端登录用户信息
 *
 * @author car
 * @since 2025-01-27
 */
@Data
@Schema(description = "用户端登录用户信息")
public class ShopLoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long uid;

    @Schema(description = "账号")
    private String account;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "用户余额")
    private BigDecimal nowMoney;

    @Schema(description = "用户积分")
    private Integer integral;

    @Schema(description = "用户经验")
    private Integer experience;

    @Schema(description = "等级")
    private Integer level;

    @Schema(description = "用户类型")
    private String userType;

    @Schema(description = "状态")
    private Integer status;
}
