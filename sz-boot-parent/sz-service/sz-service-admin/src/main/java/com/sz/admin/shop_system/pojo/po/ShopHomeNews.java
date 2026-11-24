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
 * 首页资讯配置
 *
 * @author car
 * @since 2026-03-28
 */
@Data
@Table(value = "shop_home_news", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "首页资讯配置")
public class ShopHomeNews implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "资讯id")
    private Long id;

    @Schema(description = "分类id")
    private Long categoryId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "资讯标签")
    private String tagName;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "正文内容")
    private String content;

    @Schema(description = "跳转链接")
    private String link;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "浏览量")
    private Integer browseCount;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否置顶：0-否 1-是")
    private Integer isTop;

    @Schema(description = "状态：0-正常 1-禁用")
    private Integer status;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

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
