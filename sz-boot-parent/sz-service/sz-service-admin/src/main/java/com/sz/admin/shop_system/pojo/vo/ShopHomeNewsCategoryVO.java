package com.sz.admin.shop_system.pojo.vo;

import com.sz.excel.annotation.DictFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资讯分类返回VO
 *
 * @author car
 * @since 2026-03-28
 */
@Data
@Schema(description = "资讯分类返回VO")
public class ShopHomeNewsCategoryVO {

    @Schema(description = "分类id")
    private Long id;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：0-正常 1-禁用")
    @DictFormat(dictType = "biz_status")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
