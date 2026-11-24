package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "PC端商品收藏状态VO")
public class ShopFrontUserFavoriteStatusVO {

    @Schema(description = "是否已收藏")
    private Boolean collected;

    @Schema(description = "收藏ID")
    private Long favoriteId;
}
