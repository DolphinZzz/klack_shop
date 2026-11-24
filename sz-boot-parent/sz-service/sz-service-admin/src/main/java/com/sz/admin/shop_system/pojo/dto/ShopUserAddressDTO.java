package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * ShopUserAddressDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopUserAddressDTO {

    /**
     * <p>
     * ShopUserAddress添加DTO
     * </p>
     *
     * @author car
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopUserAddress添加DTO")
    public static class Create {

       @Schema(description =  "用户id")
       private Long uid;

       @Schema(description =  "收货人姓名")
       private String realName;

       @Schema(description =  "收货人电话")
       private String phone;

       @Schema(description = "收货人所在省id")
       private Integer provinceId;

       @Schema(description =  "收货人所在省")
       private String province;

       @Schema(description =  "收货人所在市")
       private String city;

       @Schema(description = "收货人所在市id")
       private String cityId;

       @Schema(description =  "收货人所在区")
       private String district;

       @Schema(description = "收货人所在区id")
       private String districtId;

       @Schema(description =  "收货人详细地址")
       private String detail;

       @Schema(description =  "邮编")
       private String postCode;

       @Schema(description = "是否默认地址")
       private Integer isDefault;

    }

    /**
     * <p>
     * ShopUserAddress修改DTO
     * </p>
     *
     * @author car
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopUserAddress修改DTO")
    public static class Update {

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
        private String districtId;

        @Schema(description =  "收货人详细地址")
        private String detail;

        @Schema(description =  "邮编")
        private String postCode;

        @Schema(description = "是否默认地址")
        private Integer isDefault;

    }

    /**
     * <p>
     * ShopUserAddress查询DTO
     * </p>
     *
     * @author car
     * @since 2025-11-26
     */
    @Data
    @Schema(description = "ShopUserAddress查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "用户id")
        private Long uid;

        @Schema(description =  "收货人姓名")
        private String realName;

        @Schema(description =  "收货人电话")
        private String phone;

        @Schema(description =  "收货人所在省")
        private String province;

        @Schema(description =  "收货人所在市")
        private String city;

        @Schema(description =  "收货人所在区")
        private String district;

        @Schema(description =  "收货人详细地址")
        private String detail;

        @Schema(description =  "邮编")
        private String postCode;

    }
}
