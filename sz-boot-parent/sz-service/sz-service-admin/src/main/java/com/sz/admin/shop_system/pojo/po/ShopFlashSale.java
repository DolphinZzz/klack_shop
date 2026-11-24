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

/**
 * 秒杀配置表
 */
@Data
@Accessors(chain = true)
@Table(value = "shop_flash_sale", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "秒杀配置表")
public class ShopFlashSale implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long productId;

    private BigDecimal flashPrice;

    private Integer limitPerUser;

    private Integer reserveSeconds;

    private Integer status;

    private Integer sort;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDel;

    private Integer version;
}
