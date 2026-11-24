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
* 充值活动表
* </p>
*
* @author car
* @since 2025-11-26
*/
@Data
@Table(value = "shop_user_recharge_activity", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "充值活动表")
public class ShopUserRechargeActivity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="充值活动id")
    private Long id;

    @Schema(description ="活动名称")
    private String activityName;

    @Schema(description ="充值金额")
    private BigDecimal price;

    @Schema(description ="赠送金额")
    private BigDecimal givePrice;

    @Schema(description ="创建时间")
    private LocalDateTime createTime;

    @Schema(description ="修改时间")
    private LocalDateTime updateTime;

    @Schema(description ="是否删除")
    private Integer isDel;

}