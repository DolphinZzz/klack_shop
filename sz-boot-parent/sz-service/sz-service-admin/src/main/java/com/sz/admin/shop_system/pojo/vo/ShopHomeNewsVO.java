package com.sz.admin.shop_system.pojo.vo;

import com.sz.excel.annotation.DictFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页资讯返回VO
 *
 * @author car
 * @since 2026-03-28
 */
@Data
@Schema(description = "首页资讯返回VO")
public class ShopHomeNewsVO {

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
    @DictFormat(dictType = "biz_status")
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
}
