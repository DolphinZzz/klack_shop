package com.sz.admin.shop_system.agent.pojo.vo;

import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderProductVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderStatusVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 内网商城接口响应 VO
 */
public class AgentMallVO {

    @Data
    @Schema(description = "商品摘要信息")
    public static class ProductSummary {
        private Long id;
        private String productName;
        private String productInfo;
        private String image;
        private Integer cateId;
        private BigDecimal price;
        private BigDecimal otPrice;
        private Integer stock;
        private Integer sales;
        private Integer browse;
        private Integer isHot;
        private Integer isBest;
        private Integer isNew;
        private Integer isSeckill;
        private Integer isPostage;
        private BigDecimal postage;
        private String unitName;
    }

    @Data
    @Schema(description = "商品SKU信息")
    public static class ProductSku {
        private Long skuId;
        private String sku;
        private BigDecimal price;
        private BigDecimal otPrice;
        private BigDecimal flashPrice;
        private Integer stock;
        private String image;
        private Integer isDefault;
    }

    @Data
    @Schema(description = "商品详情与全量SKU信息")
    public static class ProductDetail {
        private Long id;
        private String productName;
        private String productInfo;
        private String keyword;
        private Integer cateId;
        private String image;
        private String sliderImage;
        private BigDecimal price;
        private BigDecimal otPrice;
        private Integer stock;
        private Integer sales;
        private Integer browse;
        private Integer isShow;
        private Integer isDel;
        private Integer isHot;
        private Integer isBest;
        private Integer isNew;
        private Integer isSeckill;
        private Boolean activeFlashSale;
        private Long flashSaleId;
        private Integer isPostage;
        private BigDecimal postage;
        private String unitName;
        private String desc;
        private List<ProductSku> skus;
    }

    @Data
    @Schema(description = "用户收货地址")
    public static class UserAddress {
        private Long id;
        private Long uid;
        private String realName;
        private String phone;
        private String province;
        private String city;
        private String district;
        private String detail;
        private String fullAddress;
        private String postCode;
        private Integer isDefault;
    }

    @Data
    @Schema(description = "订单发货记录")
    public static class ShipmentInfo {
        private String expressNo;
        private Long expressTmpId;
        private String expressTemplateName;
        private String expressCompanyName;
        private Integer status;
        private String statusText;
        private LocalDateTime createTime;
        private String realName;
        private String userPhone;
        private String userAddress;
    }

    @Data
    @Schema(description = "订单关联售后摘要")
    public static class AfterSaleSummary {
        private Long id;
        private String serviceNo;
        private Long orderId;
        private String orderNo;
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
    }

    @Data
    @Schema(description = "Agent 订单详情（含真实发货记录与状态时间轴）")
    public static class OrderDetail {
        private String orderNo;
        private Integer paid;
        private Integer status;
        private String statusDesc;
        private String displayTab;
        private String displayStatus;
        private Integer orderType;
        private String orderTypeLabel;
        private LocalDateTime createTime;
        private LocalDateTime payTime;
        private String payPrice;
        private String freightTotalFee;
        private Integer totalNum;
        private String realName;
        private String userPhone;
        private String userAddress;
        private String payType;
        private String expressNo;
        private String expressName;
        private Boolean canPay;
        private Boolean canConfirmReceipt;
        private Boolean canEvaluate;
        private Boolean canApplyAfterSale;
        private Long afterSaleId;
        private Integer afterSaleStatus;
        private String afterSaleStatusText;
        private List<ShopFrontOrderProductVO> orderInfoList;
        private List<ShopOrderStatusVO> orderStatusList;
        private ShipmentInfo shipment;
        private AfterSaleSummary afterSale;
        private Boolean trackingSupported;
    }

    @Data
    @Schema(description = "聊天直购建单结果")
    public static class DirectOrderResult {
        private String orderNo;
        private BigDecimal totalPrice;
        private BigDecimal freightPrice;
        private BigDecimal payPrice;
        private Long remainingSeconds;
        private String paymentUrl;
        private Integer paid;
        private Integer status;
    }
}
