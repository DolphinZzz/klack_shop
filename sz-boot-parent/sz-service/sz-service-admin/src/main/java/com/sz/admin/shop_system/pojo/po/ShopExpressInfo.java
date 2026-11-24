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
import java.time.LocalTime;

@Data
@Table(value = "shop_express_info", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "订单发货记录表")
public class ShopExpressInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "主键id")
    private Long id;

    @Schema(description = "订单id")
    private Long oid;

    @Schema(description = "快递公司模板id")
    private Long expressTmpId;

    @Schema(description = "快递单号")
    private String expressNo;

    @Schema(description = "收货人姓名")
    private String realName;

    @Schema(description = "收货人电话")
    private String userPhone;

    @Schema(description = "用户地址")
    private String userAddress;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
