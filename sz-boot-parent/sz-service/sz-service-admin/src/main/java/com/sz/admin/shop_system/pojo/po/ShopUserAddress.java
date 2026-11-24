package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.io.Serial;
import com.sz.mysql.EntityChangeListener;
import java.time.LocalDateTime;

/**
* <p>
* 用户地址表
* </p>
*
* @author car
* @since 2025-11-26
*/
@Data
@Table(value = "shop_user_address", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "用户地址表")
public class ShopUserAddress implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="用户地址id")
    private Long id;

    @Schema(description ="用户id")
    private Long uid;

    @Schema(description ="收货人姓名")
    private String realName;

    @Schema(description ="收货人电话")
    private String phone;

    @Schema(description ="收货人所在省")
    private String province;

    @Schema(description = "收货人所在省id")
    private Integer provinceId;

    @Schema(description ="收货人所在市")
    private String city;

    @Schema(description = "收货人所在市id")
    private String cityId;

    @Schema(description ="收货人所在区")
    private String district;

    @Schema(description = "收货人所在区id")
    private Integer districtId;

    @Schema(description ="收货人详细地址")
    private String detail;

    @Schema(description ="邮编")
    private String postCode;

    @Schema(description ="是否默认 0-否 1-是")
    private Integer isDefault;

    @Schema(description ="添加时间")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    private LocalDateTime updateTime;

    @Schema(description ="是否删除 0-否 1-是")
    private Integer deleted;

}
