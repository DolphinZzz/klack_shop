package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.io.Serial;
import com.sz.mysql.EntityChangeListener;
import java.time.LocalDateTime;

/**
* <p>
* 快递表
* </p>
*
* @author car
* @since 2026-01-22
*/
@Data
@Table(value = "shop_express", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "快递表")
public class ShopExpress implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="id")
    private Long id;

    @Schema(description ="编号")
    private String code;

    @Schema(description ="快递名")
    private String expressName;

    @Schema(description ="排序")
    private Integer sort;

    @Schema(description ="创建时间")
    private LocalDateTime createTime;

    @Schema(description ="修改时间")
    private LocalDateTime updateTime;

    @Schema(description ="是否删除 0-否 1-是")
    private Integer isDel;

}