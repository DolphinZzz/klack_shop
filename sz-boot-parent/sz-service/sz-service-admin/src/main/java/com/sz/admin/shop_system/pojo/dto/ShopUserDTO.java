package com.sz.admin.shop_system.pojo.dto;

import cn.idev.excel.annotation.ExcelProperty;
import com.sz.core.common.entity.PageQuery;
import com.sz.excel.annotation.DictFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.Data;

/**
 * ShopUserDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopUserDTO {

    /**
     * <p>
     * ShopUser添加DTO
     * </p>
     *
     * @author car
     * @since 2025-11-24
     */
    @Data
    @Schema(description = "ShopUser添加DTO")
    public static class Create {

       @Schema(description =  "用户账号")
       private String account;

       @Schema(description =  "用户密码")
       private String password;

       @Schema(description =  "真实姓名")
       private String realName;

       @Schema(description =  "性别，0未知，1男，2女，3保密")
       private Integer sex;

       @Schema(description =  "生日")
       private String birthday;

       @Schema(description =  "身份证")
       private String cardId;

       @Schema(description =  "用户昵称")
       private String nickname;

       @Schema(description =  "用户头像")
       private String avatar;

       @Schema(description =  "手机号")
       private String phone;

       @Schema(description =  "添加ip")
       private String addIp;

       @Schema(description =  "最后一次登录ip")
       private String lastIp;

       @Schema(description =  "用户余额")
       private BigDecimal nowMoney;

       @Schema(description =  "用户积分")
       private Integer integral;

       @Schema(description =  "用户经验")
       private Integer experience;

       @Schema(description =  "签到天数")
       private Integer signNum;

       @Schema(description =  "等级")
       private Integer level;

       @Schema(description =  "用户类型")
       private String userType;

       @Schema(description =  "状态，0-正常 1-禁用")
       private Integer status;

    }

    /**
     * <p>
     * ShopUser修改DTO
     * </p>
     *
     * @author car
     * @since 2025-11-24
     */
    @Data
    @Schema(description = "ShopUser修改DTO")
    public static class Update {

        @Schema(description =  "用户id")
        private Long uid;

        @Schema(description =  "用户账号")
        private String account;

        @Schema(description =  "用户密码")
        private String password;

        @Schema(description =  "真实姓名")
        private String realName;

        @Schema(description =  "性别，0未知，1男，2女，3保密")
        private Integer sex;

        @Schema(description =  "生日")
        private String birthday;

        @Schema(description =  "身份证")
        private String cardId;

        @Schema(description =  "用户昵称")
        private String nickname;

        @Schema(description =  "用户头像")
        private String avatar;

        @Schema(description =  "手机号")
        private String phone;

        @Schema(description =  "添加ip")
        private String addIp;

        @Schema(description =  "最后一次登录ip")
        private String lastIp;

        @Schema(description =  "用户余额")
        private BigDecimal nowMoney;

        @Schema(description =  "用户积分")
        private Integer integral;

        @Schema(description =  "用户经验")
        private Integer experience;

        @Schema(description =  "签到天数")
        private Integer signNum;

        @Schema(description =  "等级")
        private Integer level;

        @Schema(description =  "用户类型")
        private String userType;

        @Schema(description =  "状态，0-正常 1-禁用")
        private Integer status;

    }

    /**
     * <p>
     * ShopUser查询DTO
     * </p>
     *
     * @author car
     * @since 2025-11-24
     */
    @Data
    @Schema(description = "ShopUser查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "用户账号")
        private String account;

        @Schema(description =  "真实姓名")
        private String realName;

        @Schema(description =  "性别，0未知，1男，2女，3保密")
        private Integer sex;

        @Schema(description =  "身份证")
        private String cardId;

        @Schema(description =  "用户昵称")
        private String nickname;

        @Schema(description =  "手机号")
        private String phone;

        @Schema(description =  "状态，0-正常 1-禁用")
        private Integer status;

    }

    /**
     * <p>
     * ShopUser导入DTO
     * </p>
     *
     * @author car
     * @since 2025-11-24
     */
    @Data
    @Schema(description = "ShopUser导入DTO")
    public static class Import {

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

        @ExcelProperty(value = "状态，0-正常 1-禁用")
        @DictFormat(dictType = "biz_status")
        @Schema(description =  "状态，0-正常 1-禁用")
        private Integer status;

    }

    /**
     * 用户端登录DTO
     *
     * @author car
     * @since 2025-01-27
     */
    @Data
    @Schema(description = "用户端登录请求")
    public static class Login {

        @NotBlank(message = "账号或手机号不能为空")
        @Schema(description = "账号或手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
        private String accountOrPhone;

        @NotBlank(message = "密码不能为空")
        @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
        private String password;
    }

    /**
     * 用户端注册DTO
     *
     * @author car
     * @since 2025-01-27
     */
    @Data
    @Schema(description = "用户端注册请求")
    public static class Register {

        @NotBlank(message = "账号不能为空")
        @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "user001")
        private String account;

        @NotBlank(message = "密码不能为空")
        @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
        private String password;

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
        private String phone;

        @Schema(description = "真实姓名", example = "张三")
        private String realName;

        @Schema(description = "用户昵称", example = "小明")
        private String nickname;
    }

    /**
     * 用户端个人资料修改DTO
     */
    @Data
    @Schema(description = "用户端个人资料修改DTO")
    public static class FrontProfileUpdate {

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
    }

    /**
     * 用户端修改密码DTO
     */
    @Data
    @Schema(description = "用户端修改密码DTO")
    public static class FrontPassword {

        @Schema(description = "原密码")
        private String oldPassword;

        @Schema(description = "新密码")
        private String newPassword;
    }
}
