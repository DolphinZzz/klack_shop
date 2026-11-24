package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.io.Serial;
import com.sz.mysql.EntityChangeListener;
import java.time.LocalDateTime;
import java.util.List;

/**
* <p>
* 分类管理
* </p>
*
* @author car
* @since 2025-11-26
*/
@Data
@Table(value = "shop_category", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "分类管理")
public class ShopCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="分类id")
    private Long id;

    @Schema(description ="父id")
    private Integer pid;

    @Schema(description ="分类名称")
    private String cateName;

    @Schema(description ="排序")
    private Integer sort;

    @Schema(description ="图标")
    private String pic;

    @Schema(description ="添加时间")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    private LocalDateTime updateTime;

    @Schema(description ="是否删除")
    private Integer deleted;

    private List<ShopCategory> children;
}
