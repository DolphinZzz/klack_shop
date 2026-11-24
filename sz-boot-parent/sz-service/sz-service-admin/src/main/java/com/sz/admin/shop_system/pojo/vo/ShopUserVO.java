package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import com.sz.excel.annotation.DictFormat;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * ShopUser返回vo
 * </p>
 *
 * @author car
 * @since 2025-11-24
 */
@Data
@Schema(description = "ShopUser返回vo")
public class ShopUserVO {

    @ExcelIgnore
    @Schema(description =  "用户id")
    private Long uid;

    @ExcelProperty(value = "用户账号")
    @Schema(description =  "用户账号")
    private String account;

    @ExcelProperty(value = "用户密码")
    @Schema(description =  "用户密码")
    private String password;

    @ExcelProperty(value = "真实姓名")
    @Schema(description =  "真实姓名")
    private String realName;

    @ExcelProperty(value = "性别，0未知，1男，2女，3保密")
    @Schema(description =  "性别，0未知，1男，2女，3保密")
    private Integer sex;

    @ExcelProperty(value = "生日")
    @Schema(description =  "生日")
    private String birthday;

    @ExcelProperty(value = "身份证")
    @Schema(description =  "身份证")
    private String cardId;

    @ExcelProperty(value = "用户昵称")
    @Schema(description =  "用户昵称")
    private String nickname;

    @ExcelProperty(value = "用户头像")
    @Schema(description =  "用户头像")
    private String avatar;

    @ExcelProperty(value = "手机号")
    @Schema(description =  "手机号")
    private String phone;

    @ExcelProperty(value = "添加ip")
    @Schema(description =  "添加ip")
    private String addIp;

    @ExcelProperty(value = "最后一次登录ip")
    @Schema(description =  "最后一次登录ip")
    private String lastIp;

    @ExcelProperty(value = "用户余额")
    @Schema(description =  "用户余额")
    private BigDecimal nowMoney;

    @ExcelProperty(value = "用户积分")
    @Schema(description =  "用户积分")
    private Integer integral;

    @ExcelProperty(value = "用户经验")
    @Schema(description =  "用户经验")
    private Integer experience;

    @ExcelProperty(value = "签到天数")
    @Schema(description =  "签到天数")
    private Integer signNum;

    @ExcelProperty(value = "等级")
    @Schema(description =  "等级")
    private Integer level;

    @ExcelProperty(value = "用户类型")
    @Schema(description =  "用户类型")
    private String userType;

    @ExcelProperty(value = "连续签到天数")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description =  "连续签到天数")
    private LocalDateTime createTime;

    @ExcelProperty(value = "更新时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description =  "更新时间")
    private LocalDateTime updateTime;

    @ExcelProperty(value = "状态，0-正常 1-禁用")
    @DictFormat(dictType = "biz_status")
    @Schema(description =  "状态，0-正常 1-禁用")
    private Integer status;

}
