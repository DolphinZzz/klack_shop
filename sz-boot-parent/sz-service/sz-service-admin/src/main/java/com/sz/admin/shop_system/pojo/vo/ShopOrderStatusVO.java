package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * ShopOrderStatus返回vo
 * </p>
 *
 * @author car
 * @since 2026-01-29
 */
@Data
@Schema(description = "ShopOrderStatus返回vo")
public class ShopOrderStatusVO {

    @Schema(description =  "订单状态id")
    private Long id;

    @Schema(description =  "订单id")
    private Long oid;

    @Schema(description =  "操作类型")
    private String changeType;

    @Schema(description =  "操作备注")
    private String changeMessage;

    @Schema(description =  "操作时间")
    private LocalDateTime createTime;

}