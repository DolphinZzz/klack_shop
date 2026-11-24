package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * ShopExpress返回vo
 * </p>
 *
 * @author car
 * @since 2026-01-22
 */
@Data
@Schema(description = "ShopExpress返回vo")
public class ShopExpressVO {

    @Schema(description =  "id")
    private Long id;

    @Schema(description =  "编号")
    private String code;

    @Schema(description =  "快递名")
    private String expressName;

    @Schema(description =  "排序")
    private Integer sort;

    @Schema(description =  "创建时间")
    private LocalDateTime createTime;

    @Schema(description =  "修改时间")
    private LocalDateTime updateTime;

}