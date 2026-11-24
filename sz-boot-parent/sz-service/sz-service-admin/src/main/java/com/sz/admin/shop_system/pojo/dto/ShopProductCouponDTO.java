package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * ShopProductCouponDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopProductCouponDTO {

    /**
     * <p>
     * ShopProductCoupon添加DTO
     * </p>
     *
     * @author car
     * @since 2026-01-30
     */
    @Data
    @Schema(description = "ShopProductCoupon添加DTO")
    public static class Create {

       @Schema(description =  "优惠券名称")
       private String name;

       @Schema(description =  "优惠券面值")
       private BigDecimal money;

       @Schema(description =  "发放总数")
       private Integer total;

       @Schema(description =  "领取时间")
       @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
       private LocalDateTime couponStartTime;

       @Schema(description =  "结束时间")
       @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
       private LocalDateTime couponEndTime;

       @Schema(description =  "可使用开始时间")
       @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
       private LocalDateTime useStartTime;

       @Schema(description =  "可使用结束时间")
       @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
       private LocalDateTime useEndTime;

       @Schema(description =  "优惠券类型")
       private Integer type;

       @Schema(description =  "所属商品id")
       private String cateKey;

       @Schema(description =  "最低消费")
       private BigDecimal minMoney;

    }

    /**
     * <p>
     * ShopProductCoupon修改DTO
     * </p>
     *
     * @author car
     * @since 2026-01-30
     */
    @Data
    @Schema(description = "ShopProductCoupon修改DTO")
    public static class Update {

        @Schema(description =  "优惠券id")
        private Long id;

        @Schema(description =  "优惠券名称")
        private String name;

        @Schema(description =  "优惠券面值")
        private BigDecimal money;

        @Schema(description =  "发放总数")
        private Integer total;

        @Schema(description =  "领取时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime couponStartTime;

        @Schema(description =  "结束时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime couponEndTime;

        @Schema(description =  "可使用开始时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime useStartTime;

        @Schema(description =  "可使用结束时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime useEndTime;

        @Schema(description =  "优惠券类型")
        private Integer type;

        @Schema(description =  "所属商品id")
        private String cateKey;

        @Schema(description =  "最低消费")
        private BigDecimal minMoney;

    }

    /**
     * <p>
     * ShopProductCoupon查询DTO
     * </p>
     *
     * @author car
     * @since 2026-01-30
     */
    @Data
    @Schema(description = "ShopProductCoupon查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "优惠券名称")
        private String name;

        @Schema(description =  "优惠券类型")
        private Integer type;

        @Schema(description =  "状态，0-否，1是")
        private Integer status;

    }

    @Data
    @Schema(description = "PC端我的优惠券查询DTO")
    public static class FrontUserList extends PageQuery {

        @Schema(description = "标签页 all-全部 unused-未使用 used_expired-已使用或失效")
        private String tab;
    }
}
