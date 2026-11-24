package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "管理端售后单VO")
public class ShopOrderAfterSaleVO {

    private Long id;

    private String serviceNo;

    private Long orderId;

    private String orderNo;

    private Long uid;

    private String realName;

    private String userPhone;

    private String userAddress;

    private Integer afterSaleType;

    private String afterSaleTypeText;

    private Integer status;

    private String statusText;

    private BigDecimal applyAmount;

    private String reason;

    private String userRemark;

    private String auditRemark;

    private String receiveRemark;

    private String returnExpressName;

    private String returnExpressNo;

    private String returnRemark;

    private LocalDateTime auditTime;

    private LocalDateTime returnTime;

    private LocalDateTime receiveTime;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;

    private List<ShopFrontOrderProductVO> orderInfoList;
}