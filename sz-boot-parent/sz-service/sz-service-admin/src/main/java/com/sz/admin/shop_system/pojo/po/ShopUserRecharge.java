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
* 用户充值表
* </p>
*
* @author car
* @since 2025-11-26
*/
@Data
@Table(value = "shop_user_recharge", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "用户充值表")
public class ShopUserRecharge implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="id")
    private Long id;

    @Schema(description ="充值用户id")
    private Long uid;

    @Schema(description ="充值活动id")
    private Long payActivityId;

    @Schema(description ="充值订单id")
    private Long orderId;

    @Schema(description ="充值金额")
    private BigDecimal price;

    @Schema(description ="赠送金额")
    private BigDecimal givePrice;

    @Schema(description ="充值类型")
    private String rechargeType;

    @Schema(description ="充值时间")
    private LocalDateTime payTime;

    @Schema(description ="退款金额")
    private BigDecimal refundPrice;

    @Schema(description ="创建时间")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    private LocalDateTime updateTime;

    @Schema(description ="是否删除 0-否 1-是")
    private Integer isDel;

}
