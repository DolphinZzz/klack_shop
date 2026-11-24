package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.sz.mysql.EntityChangeListener;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户积分流水表
 * </p>
 */
@Data
@Table(value = "shop_user_integral_bill", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "用户积分流水表")
public class ShopUserIntegralBill implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "id")
    private Long id;

    @Schema(description = "用户id")
    private Long uid;

    @Schema(description = "关联id")
    private String linkId;

    @Schema(description = "收支，0支出，1收入")
    private Integer pm;

    @Schema(description = "账单标题")
    private String title;

    @Schema(description = "明细种类")
    private String category;

    @Schema(description = "明细类型")
    private String type;

    @Schema(description = "明细数字")
    private Integer number;

    @Schema(description = "剩余积分")
    private Integer balance;

    @Schema(description = "备注")
    private String mark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "状态")
    private Integer status;
}
