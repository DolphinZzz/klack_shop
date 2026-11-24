package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "前端城市选择VO")
public class ShopSystemCityVO {

    @Schema(description = "主键ID")
    private Integer id;

    @Schema(description = "城市ID")
    private Integer cityId;

    @Schema(description = "父级城市ID")
    private Integer parentId;

    @Schema(description = "区域类型：1省 2市 3区 4街道")
    private Integer regionType;

    @Schema(description = "名称")
    private String name;
}
