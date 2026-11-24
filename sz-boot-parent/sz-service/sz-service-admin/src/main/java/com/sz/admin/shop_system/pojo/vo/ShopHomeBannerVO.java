package com.sz.admin.shop_system.pojo.vo;

import com.sz.excel.annotation.DictFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页轮播返回VO
 *
 * @author car
 * @since 2026-02-26
 */
@Data
@Schema(description = "首页轮播返回VO")
public class ShopHomeBannerVO {

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
    @DictFormat(dictType = "biz_status")
    private Integer status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
