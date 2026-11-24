package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "PC端用户积分明细VO")
public class ShopFrontUserIntegralBillVO {

    @Schema(description = "流水ID")
    private Long id;

    @Schema(description = "关联ID")
    private String linkId;

    @Schema(description = "收支，0支出，1收入")
    private Integer pm;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "积分值")
    private Integer number;

    @Schema(description = "当前积分余额")
    private Integer balance;

    @Schema(description = "备注")
    private String mark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
