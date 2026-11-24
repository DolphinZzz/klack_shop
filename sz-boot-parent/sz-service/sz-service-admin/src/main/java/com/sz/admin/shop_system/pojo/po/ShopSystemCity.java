package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 城市表
 */
@Data
@Table("shop_system_city")
@Schema(description = "城市表")
public class ShopSystemCity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.None)
    private Integer id;

    @Schema(description = "城市ID")
    private Integer cityId;

    @Schema(description = "层级（库内字段：level）")
    private Integer level;

    @Schema(description = "父级城市ID（对应上级 city_id）")
    private Integer parentId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "是否展示")
    private Integer isShow;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
