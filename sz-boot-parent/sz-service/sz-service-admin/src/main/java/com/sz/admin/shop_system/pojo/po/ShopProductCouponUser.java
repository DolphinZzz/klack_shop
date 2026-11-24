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
* 优惠券使用表
* </p>
*
* @author car
* @since 2026-02-10
*/
@Data
@Table(value = "shop_product_coupon_user", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "优惠券使用表")
public class ShopProductCouponUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="id")
    private Long id;

    @Schema(description ="优惠券id")
    private Long cid;

    @Schema(description ="领取人id")
    private Long uid;

    @Schema(description ="获取方式 send后台发放，get用户领取")
    private String type;

    @Schema(description ="状态（0：未使用，1：已使用, 2：已过期）")
    private Integer status;

    @Schema(description ="优惠券创建时间")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    private LocalDateTime updateTime;

    @Schema(description ="使用时间")
    private LocalDateTime useTime;

    @Schema(description ="使用类型 1全场通用，2商品券，3品类券")
    private Integer useType;

}
