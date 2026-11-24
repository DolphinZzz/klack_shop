package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "PC端用户收藏VO")
public class ShopFrontUserFavoriteVO {

    @Schema(description = "收藏ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品简介")
    private String productInfo;

    @Schema(description = "商品图片")
    private String productImage;

    @Schema(description = "商品价格")
    private BigDecimal productPrice;

    @Schema(description = "赠送积分")
    private BigDecimal giveIntegral;

    @Schema(description = "收藏时间")
    private LocalDateTime createTime;

    @Schema(description = "最近更新时间")
    private LocalDateTime updateTime;
}
