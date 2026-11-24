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
* 用户帐单表
* </p>
*
* @author sz-admin
* @since 2025-11-26
*/
@Data
@Table(value = "shop_user_bill", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "用户帐单表")
public class ShopUserBill implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Schema(description ="id")
    private Long id;

    @Schema(description ="用户id")
    private Long uid;

    @Schema(description ="关联id")
    private String linkId;

    @Schema(description ="收支")
    private Integer pm;

    @Schema(description ="账单标题")
    private String title;

    @Schema(description ="明细种类")
    private String category;

    @Schema(description ="明细类型")
    private String type;

    @Schema(description ="明细数字")
    private BigDecimal number;

    @Schema(description ="剩余")
    private BigDecimal balance;

    @Schema(description ="备注")
    private String mark;

    @Schema(description ="创建时间")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    private LocalDateTime updateTime;

    @Schema(description ="状态")
    private Integer status;

}