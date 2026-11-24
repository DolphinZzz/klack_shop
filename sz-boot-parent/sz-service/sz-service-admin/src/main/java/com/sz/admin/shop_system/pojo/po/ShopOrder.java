package com.sz.admin.shop_system.pojo.po;

import com.mybatisflex.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.io.Serial;
import com.sz.mysql.EntityChangeListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
* <p>
* 订单表
* </p>
*
* @author car
* @since 2026-01-28
*/
@Data
@Accessors(chain = true)
@Table(value = "shop_order", onInsert = EntityChangeListener.class, onUpdate = EntityChangeListener.class)
@Schema(description = "订单表")
public class ShopOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description ="订单id")
    private Long id;

    @Schema(description ="订单号")
    private String orderId;

    @Schema(description ="用户id")
    private Long uid;

    @Schema(description ="用户姓名")
    private String realName;

    @Schema(description ="用户电话")
    private String userPhone;

    @Schema(description ="用户地址")
    private String userAddress;

    @Schema(description ="购物车id")
    private String cartIds;

    @Schema(description ="运费金额")
    private BigDecimal freightPrice;

    @Schema(description ="订单商品总数")
    private Integer totalNum;

    @Schema(description ="订单总价")
    private BigDecimal totalPrice;

    @Schema(description = "实际价格")
    private BigDecimal payPrice;

    @Schema(description ="抵扣金额")
    private BigDecimal deductionPrice;

    @Schema(description ="消费券id")
    private Long couponId;

    @Schema(description ="消费券金额")
    private BigDecimal couponPrice;

    @Schema(description ="支付状态")
    private Integer paid;

    @Schema(description ="支付时间")
    private LocalDateTime payTime;

    @Schema(description ="支付方式")
    private String payType;

    @Schema(description ="订单状态")
    private Integer status;

    @Schema(description ="退款状态")
    private Integer refundStatus;

    @Schema(description ="用户退款说明")
    private String refundReasonExplain;

    @Schema(description ="不退款理由")
    private String refundReason;

    @Schema(description ="退款金额")
    private BigDecimal refundPrice;

    @Schema(description ="快递单号")
    private String expressNo;

    @Schema(description ="发货id")
    private Long deliveryId;

    @Schema(description ="消费赚取积分")
    private Integer gainIntegral;

    @Schema(description ="消费使用积分")
    private Integer useIntegral;

    @Schema(description ="是否删除")
    private Integer isDel;

    @Schema(description ="拼团id")
    private Integer pinkId;

    @Schema(description ="秒杀id")
    private Integer seckillId;

    @Schema(description ="砍价id")
    private Integer bargainId;

    @Schema(description ="创建时间")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    private LocalDateTime updateTime;

}
