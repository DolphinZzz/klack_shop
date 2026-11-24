package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.io.Serial;
import com.sz.mysql.EntityChangeListener;
import java.math.BigDecimal;

/**
* <p>
* 快递模板表
* </p>
*
* @author car
* @since 2026-01-22
*/
@Data
@Table(value = "shop_express_template", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "快递模板表")
public class ShopExpressTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="快递模板id")
    private Long id;

    @Schema(description ="快递名称")
    private String name;

    @Schema(description ="快递id")
    private Long expressId;

    @Schema(description ="价格")
    private BigDecimal expressPrice;

    @Schema(description ="是否删除 0-否 1-是")
    private Integer isDel;

}