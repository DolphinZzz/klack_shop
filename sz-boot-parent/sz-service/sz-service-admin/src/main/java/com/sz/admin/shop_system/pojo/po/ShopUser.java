package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.io.Serial;
import com.sz.mysql.EntityChangeListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
* <p>
* 用户表
* </p>
*
* @author car
* @since 2025-11-24
*/
@Data
@Table(value = "shop_user", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "用户表")
public class ShopUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="用户id")
    private Long uid;

    @Schema(description ="用户账号")
    private String account;

    @Schema(description ="用户密码")
    private String password;

    @Schema(description ="真实姓名")
    private String realName;

    @Schema(description ="性别，0未知，1男，2女，3保密")
    private Integer sex;

    @Schema(description ="生日")
    private String birthday;

    @Schema(description ="身份证")
    private String cardId;

    @Schema(description ="用户昵称")
    private String nickname;

    @Schema(description ="用户头像")
    private String avatar;

    @Schema(description ="手机号")
    private String phone;

    @Schema(description ="添加ip")
    private String addIp;

    @Schema(description ="最后一次登录ip")
    private String lastIp;

    @Schema(description ="用户余额")
    private BigDecimal nowMoney;

    @Schema(description ="用户积分")
    private Integer integral;

    @Schema(description ="用户经验")
    private Integer experience;

    @Schema(description ="签到天数")
    private Integer signNum;

    @Schema(description ="等级")
    private Integer level;

    @Schema(description ="用户类型")
    private String userType;

    @Schema(description ="连续签到天数")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    private LocalDateTime updateTime;

    @Schema(description ="状态，0-正常 1-禁用")
    private Integer status;

}
