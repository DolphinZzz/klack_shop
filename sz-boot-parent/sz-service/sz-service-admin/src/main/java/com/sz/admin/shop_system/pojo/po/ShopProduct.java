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
* 商品表
* </p>
*
* @author car
* @since 2025-12-03
*/
@Data
@Table(value = "shop_product", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "商品表")
public class ShopProduct implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="商品id")
    private Long id;

    @Schema(description ="商品图片")
    private String image;

    @Schema(description ="轮播图")
    private String sliderImage;

    @Schema(description ="商品名称")
    private String productName;

    @Schema(description ="商品介绍")
    private String productInfo;

    @Schema(description ="关键词")
    private String keyword;

    @Schema(description ="分类id")
    private Integer cateId;

    @Schema(description ="商品价格")
    private BigDecimal price;

    @Schema(description ="vip价格")
    private BigDecimal vipPrice;

    @Schema(description ="市场价")
    private BigDecimal otPrice;

    @Schema(description ="是否包邮")
    private Integer isPostage;

    @Schema(description = "快递公司名")
    private String expressName;

    @Schema(description = "快递模板id")
    private Integer expressId;

    @Schema(description ="邮费")
    private BigDecimal postage;

    @Schema(description ="单位名")
    private String unitName;

    @Schema(description ="排序")
    private Integer sort;

    @Schema(description ="销量")
    private Integer sales;

    @Schema(description ="库存")
    private Integer stock;

    @Schema(description ="状态")
    private Integer isShow;

    @Schema(description ="是否热卖")
    private Integer isHot;

    @Schema(description ="是否精品")
    private Integer isBest;

    @Schema(description ="是否新品")
    private Integer isNew;

    @Schema(description ="开启秒杀")
    private Integer isSeckill;

    @Schema(description ="开启拼团")
    private Integer isBargain;

    @Schema(description ="赠送积分，按商品金额x100计算")
    private BigDecimal giveIntegral;

    @Schema(description ="产品描述")
    private String desc;

    @Schema(description ="浏览量")
    private Integer browse;

    @Schema(description ="虚拟销量")
    private Integer fictiBrowse;

    @Schema(description ="活动显示排序")
    private String activity;

    @Schema(description ="创建时间")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    private LocalDateTime updateTime;

    @Schema(description ="删除")
    private Integer isDel;

    @Schema(description ="并发版本控制")
    private Integer version;

}
