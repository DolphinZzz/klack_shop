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
 * 首页轮播配置
 *
 * @author car
 * @since 2026-02-26
 */
@Data
@Table(value = "shop_home_banner", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "首页轮播配置")
public class ShopHomeBanner implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "轮播id")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "图片")
    private String image;

    @Schema(description = "跳转链接")
    private String link;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：0-正常 1-禁用")
    private Integer status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "删除标记：0-否 1-是")
    private Integer isDel;

    @Schema(description = "并发版本")
    private Integer version;
}
