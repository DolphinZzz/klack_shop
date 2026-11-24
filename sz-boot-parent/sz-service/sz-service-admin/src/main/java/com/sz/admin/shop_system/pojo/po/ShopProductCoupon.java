package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.io.Serial;
import com.sz.mysql.EntityChangeListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
* <p>
* 商品优惠券表
* </p>
*
* @author car
* @since 2026-01-30
*/
@Data
@Table(value = "shop_product_coupon", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "商品优惠券表")
public class ShopProductCoupon implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="优惠券id")
    private Long id;

    @Schema(description ="优惠券名称")
    private String name;

    @Schema(description ="优惠券面值")
    private BigDecimal money;

    @Schema(description ="发放总数")
    private Integer total;

    @Schema(description ="剩余数量")
    private Integer lastTotal;

    @Schema(description ="领取时间")
    private LocalDateTime couponStartTime;

    @Schema(description ="结束时间")
    private LocalDateTime couponEndTime;

    @Schema(description ="可使用开始时间")
    private LocalDateTime useStartTime;

    @Schema(description ="可使用结束时间")
    private LocalDateTime useEndTime;

    @Schema(description ="优惠券类型")
    private Integer type;

    @Schema(description ="所属商品id")
    private String cateKey;

    @Schema(description ="最低消费")
    private BigDecimal minMoney;

    @Schema(description ="创建时间")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    private LocalDateTime updateTime;

    @Schema(description ="状态，0-否，1是")
    private Integer status;

}