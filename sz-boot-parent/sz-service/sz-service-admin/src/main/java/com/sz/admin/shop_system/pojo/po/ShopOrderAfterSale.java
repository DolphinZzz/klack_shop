package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.sz.mysql.EntityChangeListener;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@Table(value = "shop_order_after_sale", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "订单售后表")
public class ShopOrderAfterSale implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String serviceNo;

    private Long orderId;

    private String orderNo;

    private Long uid;

    private Integer afterSaleType;

    private Integer status;

    private BigDecimal applyAmount;

    private String reason;

    private String userRemark;

    private String auditRemark;

    private String receiveRemark;

    private String returnExpressName;

    private String returnExpressNo;

    private String returnRemark;

    private LocalDateTime auditTime;

    private LocalDateTime returnTime;

    private LocalDateTime receiveTime;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDel;

    private Integer version;
}