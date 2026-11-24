package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * ShopUserAddress返回vo
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
@Data
@Schema(description = "ShopUserAddress返回vo")
public class ShopUserAddressVO {

    @Schema(description =  "用户地址id")
    private Long id;

    @Schema(description =  "用户id")
    private Long uid;

    @Schema(description =  "收货人姓名")
    private String realName;

    @Schema(description =  "收货人电话")
    private String phone;

    @Schema(description =  "收货人所在省")
    private String province;

    @Schema(description = "收货人所在省id")
    private Integer provinceId;

    @Schema(description =  "收货人所在市")
    private String city;

    @Schema(description = "收货人所在市id")
    private String cityId;

    @Schema(description =  "收货人所在区")
    private String district;

    @Schema(description = "收货人所在区id")
    private Integer districtId;

    @Schema(description =  "收货人详细地址")
    private String detail;

    @Schema(description =  "邮编")
    private String postCode;

    @Schema(description ="是否默认 0-否 1-是")
    private Integer isDefault;

}
