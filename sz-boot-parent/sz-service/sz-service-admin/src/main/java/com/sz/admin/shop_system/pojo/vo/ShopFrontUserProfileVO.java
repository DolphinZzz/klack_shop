package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户端个人资料VO
 */
@Data
@Schema(description = "用户端个人资料VO")
public class ShopFrontUserProfileVO {

    @Schema(description = "用户id")
    private Long uid;

    @Schema(description = "用户账号")
    private String account;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "性别，0未知，1男，2女，3保密")
    private Integer sex;

    @Schema(description = "生日")
    private String birthday;

    @Schema(description = "身份证")
    private String cardId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "状态，0-正常 1-禁用")
    private Integer status;
}
