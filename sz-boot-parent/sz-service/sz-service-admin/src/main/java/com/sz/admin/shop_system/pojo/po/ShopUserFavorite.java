package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.sz.mysql.EntityChangeListener;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table(value = "shop_user_favorite", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "用户商品收藏表")
public class ShopUserFavorite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "收藏id")
    private Long id;

    @Schema(description = "用户id")
    private Long uid;

    @Schema(description = "商品id")
    private Long productId;

    @Schema(description = "商品名称快照")
    private String productName;

    @Schema(description = "商品简介快照")
    private String productInfo;

    @Schema(description = "商品图片快照")
    private String productImage;

    @Schema(description = "商品价格快照")
    private BigDecimal productPrice;

    @Schema(description = "商品赠送积分快照")
    private BigDecimal giveIntegral;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除，0否，1是")
    private Integer isDel;

    @Schema(description = "并发版本控制")
    private Integer version;
}
