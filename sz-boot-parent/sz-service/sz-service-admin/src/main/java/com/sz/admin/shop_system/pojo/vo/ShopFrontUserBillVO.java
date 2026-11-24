package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "PC端用户余额账单VO")
public class ShopFrontUserBillVO {

    @Schema(description = "账单ID")
    private Long id;

    @Schema(description = "关联ID")
    private String linkId;

    @Schema(description = "收支，0支出，1收入")
    private Integer pm;

    @Schema(description = "账单标题")
    private String title;

    @Schema(description = "账单类型")
    private String type;

    @Schema(description = "账单金额")
    private BigDecimal number;

    @Schema(description = "账户余额")
    private BigDecimal balance;

    @Schema(description = "备注")
    private String mark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
