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
* 订单状态表
* </p>
*
* @author car
* @since 2026-01-29
*/
@Data
@Table(value = "shop_order_status", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "订单状态表")
public class ShopOrderStatus implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="订单状态id")
    private Long id;

    @Schema(description ="订单id")
    private Long oid;

    @Schema(description ="操作类型")
    private String changeType;

    @Schema(description ="操作备注")
    private String changeMessage;

    @Schema(description ="操作时间")
    private LocalDateTime createTime;

}