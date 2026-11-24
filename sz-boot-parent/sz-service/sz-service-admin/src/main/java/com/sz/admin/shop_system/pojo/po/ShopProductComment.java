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
import java.time.LocalDateTime;

/**
 * <p>
 * 商品评论表
 * </p>
 *
 * @author car
 * @since 2026-03-24
 */
@Data
@Accessors(chain = true)
@Table(value = "shop_product_comment", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "商品评论表")
public class ShopProductComment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "评论id")
    private Long id;

    @Schema(description = "用户id")
    private Long uid;

    @Schema(description = "订单id")
    private Long oid;

    @Schema(description = "商品id")
    private Long productId;

    @Schema(description = "商品评分")
    private Integer productScore;

    @Schema(description = "服务评分")
    private Integer serviceScore;

    @Schema(description = "评论内容")
    private String comment;

    @Schema(description = "评论图片")
    private String pic;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除")
    private Integer isDel;
}