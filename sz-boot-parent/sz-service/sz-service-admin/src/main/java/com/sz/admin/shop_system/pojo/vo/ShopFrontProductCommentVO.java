package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "PC端我的评论列表VO")
public class ShopFrontProductCommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "评论用户名称")
    private String userName;

    @Schema(description = "评论用户头像")
    private String userAvatar;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String productImage;

    @Schema(description = "商品简介")
    private String productInfo;

    @Schema(description = "商品价格")
    private BigDecimal productPrice;

    @Schema(description = "商品评分")
    private Integer productScore;



    @Schema(description = "评论内容")
    private String comment;

    @Schema(description = "晒单图片，多个用英文逗号分隔")
    private String pic;

    @Schema(description = "评论时间")
    private LocalDateTime createTime;
}
