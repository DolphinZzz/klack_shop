package com.sz.admin.shop_system.pojo.dto;

import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * ShopProductDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopProductDTO {

    /**
     * <p>
     * ShopProduct添加DTO
     * </p>
     *
     * @author car
     * @since 2025-12-03
     */
    @Data
    @Schema(description = "ShopProduct添加DTO")
    public static class Create {

       @Schema(description =  "商品图片")
       private String image;

       @Schema(description =  "商品名称")
       private String productName;

       @Schema(description =  "商品介绍")
       private String productInfo;

       @Schema(description =  "关键词")
       private String keyword;

       @Schema(description =  "商品价格")
       private BigDecimal price;

       @Schema(description =  "vip价格")
       private BigDecimal vipPrice;

       @Schema(description =  "市场价")
       private BigDecimal otPrice;

       @Schema(description =  "是否包邮")
       private Integer isPostage;

       @Schema(description = "快递公司名")
       private String expressName;

       @Schema(description = "快递模板id")
       private Integer expressId;

       @Schema(description =  "邮费")
       private BigDecimal postage;

       @Schema(description =  "单位名")
       private String unitName;

       @Schema(description =  "状态")
       private Integer isShow;

       @Schema(description =  "是否热卖")
       private Integer isHot;

       @Schema(description =  "是否精品")
       private Integer isBest;

       @Schema(description =  "是否新品")
       private Integer isNew;

       @Schema(description =  "开启秒杀")
       private Integer isSeckill;

       @Schema(description =  "开启拼团")
       private Integer isBargain;

       @Schema(description =  "赠送积分，系统按商品金额x100自动计算")
       private BigDecimal giveIntegral;

       @Schema(description =  "产品描述")
       private String desc;

       @Schema(description =  "活动显示排序")
       private String activity;

       @Schema(description =  "删除")
       private Integer isDel;

       @Schema(description =  "多个分类id")
       private List<Integer> cateIds;

       @Schema(description = "商品分类id")
       private String cateId;

       @Schema(description = "轮播图")
       private List<String> sliderImages;

       @Schema(description = "商品属性")
       private List<ShopProductAttrDTO.Create> attrList;

       @Schema(description = "商品属性值")
       private List<ShopProductAttrDTO.Value> attrValueList;


    }

    /**
     * <p>
     * ShopProduct修改DTO
     * </p>
     *
     * @author car
     * @since 2025-12-03
     */
    @Data
    @Schema(description = "ShopProduct修改DTO")
    public static class Update {

        @Schema(description =  "商品id")
        private Long id;

        @Schema(description =  "商品图片")
        private String image;

        @Schema(description =  "商品名称")
        private String productName;

        @Schema(description =  "商品介绍")
        private String productInfo;

        @Schema(description =  "关键词")
        private String keyword;

        @Schema(description =  "分类id")
        private Integer cateId;

        @Schema(description =  "商品价格")
        private BigDecimal price;

        @Schema(description =  "vip价格")
        private BigDecimal vipPrice;

        @Schema(description =  "市场价")
        private BigDecimal otPrice;

        @Schema(description =  "是否包邮")
        private Integer isPostage;

        @Schema(description = "快递公司名")
        private String expressName;

        @Schema(description = "快递模板id")
        private Integer expressId;

        @Schema(description =  "邮费")
        private BigDecimal postage;

        @Schema(description =  "单位名")
        private String unitName;

        @Schema(description =  "销量")
        private Integer sales;

        @Schema(description =  "库存")
        private Integer stock;

        @Schema(description =  "状态")
        private Integer isShow;

        @Schema(description =  "是否热卖")
        private Integer isHot;

        @Schema(description =  "是否精品")
        private Integer isBest;

        @Schema(description =  "是否新品")
        private Integer isNew;

        @Schema(description =  "开启秒杀")
        private Integer isSeckill;

        @Schema(description =  "开启拼团")
        private Integer isBargain;

        @Schema(description =  "赠送积分，系统按商品金额x100自动计算")
        private BigDecimal giveIntegral;

        @Schema(description =  "产品描述")
        private String desc;

        @Schema(description =  "活动显示排序")
        private String activity;

        @Schema(description =  "删除")
        private Integer isDel;

        @Schema(description = "轮播图")
        private List<String> sliderImages;

        @Schema(description = "商品属性")
        private List<ShopProductAttrDTO.Create> attrList;

        @Schema(description = "商品属性值")
        private List<ShopProductAttrDTO.Value> attrValueList;

    }

    /**
     * <p>
     * ShopProduct查询DTO
     * </p>
     *
     * @author car
     * @since 2025-12-03
     */
    @Data
    @Schema(description = "ShopProduct查询DTO")
    public static class ListQuery extends PageQuery {

        @Schema(description =  "商品名称")
        private String productName;

        @Schema(description =  "分类id")
        private Integer cateId;

        @Schema(description =  "状态")
        private Integer isShow;

    }

    /**
     * Created by IntelliJ IDEA.
     *
     * @Author car
     * @create 2025/12/4
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "商品列表请求")
    public static class FrontList extends PageQuery implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "搜索关键词")
        private String keyword;

        @Schema(description = "分类id")
        private String cid;

        @Schema(description = "价格排序", allowableValues = "range[asc,desc]")
        private String priceOrder = "asc";

        @Schema(description = "商品排序", allowableValues = "range[asc,desc]")
        private String salesOrder = "asc";

        @Schema(description = "最低价格")
        private BigDecimal minPrice;

        @Schema(description = "最高价格")
        private BigDecimal maxPrice;

    }

    @Data
    @Schema(description = "商品详情评论分页DTO")
    public static class FrontCommentPage extends PageQuery {
    }
}
