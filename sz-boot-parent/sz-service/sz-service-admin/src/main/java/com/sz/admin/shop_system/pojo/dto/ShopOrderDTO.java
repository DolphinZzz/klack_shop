package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

/**
 * ShopOrderDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopOrderDTO {

    /**
     * <p>
     * ShopOrder查询DTO
     * </p>
     *
     * @author car
     * @since 2026-01-28
     */
    @Data
    @Schema(description = "ShopOrder查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "订单号")
        private String orderId;

        @Schema(description = "用户姓名")
        private String realName;

        @Schema(description =  "订单类型(0普通 1秒杀 2砍价 3拼团)")
        private Integer orderType;

        @Schema(description =  "订单状态筛选(0:待发货 1:待收货 2:已收货待评价 3:已完成 4:退款；不传为全部)")
        private Integer status;

    }

    /**
     * <p>
     * ShopOrder修改DTO
     * </p>
     *
     * @author car
     * @since 2026-01-28
     */
    @Data
    @Schema(description = "ShopOrder修改DTO")
    public static class Update {

        @Schema(description =  "订单id")
        private Long id;

        @Schema(description = "订单状态")
        private Integer status;

    }

    @Data
    @Schema(description = "订单发货DTO")
    public static class Deliver {

        @Schema(description = "订单号")
        private String orderNo;

        @Schema(description = "快递模板id")
        private Long expressTemplateId;
    }

    /**
     * Created by IntelliJ IDEA.
     *
     * @Author car
     * @create 2026/1/29
     */
    @Data
    @Schema(description = "用户下单DTO")
    public static class PreOrder {
        @Schema(description = "购物车ids")
        private List<Integer> shoppingCartIds;
        @Schema(description = "下单类型")
        private String preOrderType;
    }

    /**
     * Created by IntelliJ IDEA.
     *
     * @Author car
     * @create 2026/2/11
     */
    @Data
    @Schema(description = "预订单重算金额DTO")
    public static class Recalculate {

        @Schema(description = "地址id")
        private Long addressId;

        @Schema(description = "是否使用积分")
        private Boolean isUseIntegral;

        @Schema(description = "预订单号")
        private String preOrderNo;

        @Schema(description = "平台用户券id")
        private Long platUserCouponId;

        @Schema(description = "用户券id")
        private Long userCouponId;
    }

    /**
     * <p>
     * ShopOrder添加DTO
     * </p>
     *
     * @author car
     * @since 2026-01-28
     */
    @Data
    @Schema(description = "确认下单DTO")
    public static class Create {
       private String addressId;

       private Boolean isUseIntegral;

       private Integer platUserCouponId;

       private String preOrderNo;

    }

    /**
     * Created by IntelliJ IDEA.
     *
     * @Author car
     * @create 2026/2/25
     */
    @Data
    @Schema(description = "订单支付DTO")
    public static class Payment {
        @Schema(description = "订单号")
        private String orderNo;
        @Schema(description = "支付类型")
        private String payType;
        @Schema(description = "支付方式")
        private String payChannel;
    }

    @Data
    @Schema(description = "PC端订单中心查询DTO")
    public static class FrontList extends PageQuery {

        @Schema(description = "标签页 all-全部 pending_payment-待付款 pending_shipment-待发货 pending_receipt-待收货 cancelled-已取消")
        private String tab;

        @Schema(description = "搜索关键词，支持订单号或商品名称")
        private String keyword;
    }

    @Data
    @Schema(description = "PC端订单评价DTO")
    public static class FrontEvaluate {

        @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
        private String orderNo;

        @Schema(description = "评分，1-5")
        private Integer score;

        @Schema(description = "评价内容")
        private String content;
    }

    @Data
    @Schema(description = "PC端我的评论查询DTO")
    public static class FrontCommentList extends PageQuery {

        @Schema(description = "搜索关键词，支持商品名称、订单号、评论内容")
        private String keyword;
    }
}
