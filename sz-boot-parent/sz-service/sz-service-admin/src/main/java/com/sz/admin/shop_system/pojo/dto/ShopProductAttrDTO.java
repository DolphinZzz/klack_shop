package com.sz.admin.shop_system.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sz.core.common.entity.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * ShopProductAttrDTO 聚合请求 DTO（静态内部类归组）
 */
public class ShopProductAttrDTO {

    /**
     * <p>
     * ShopProductAttr添加DTO
     * </p>
     *
     * @author car
     * @since 2025-12-04
     */
    @Data
    @Schema(description = "ShopProductAttr添加DTO")
    public static class Create {

        @Schema(description = "属性名")
        private String attrName;

        @Schema(description = "属性值")
        private String attrValues;

        @Schema(description = "活动类型，0商品，1秒杀，2砍价")
        private Integer type;

        @Schema(description = "是否删除，0否，1是")
        private Integer isDel;

        @Schema(description = "是否有图片")
        private Boolean isShowImage;

        List<Option> optionList;

    }

    /**
     * <p>
     * ShopProductAttr修改DTO
     * </p>
     *
     * @author car
     * @since 2025-12-04
     */
    @Data
    @Schema(description = "ShopProductAttr修改DTO")
    public static class Update {

        @Schema(description =  "主键")
        private Long id;

        @Schema(description =  "商品id")
        private Integer productId;

        @Schema(description =  "属性名")
        private String attrName;

        @Schema(description =  "属性值")
        private String attrValues;

        @Schema(description =  "活动类型，0商品，1秒杀，2砍价")
        private Integer type;

        @Schema(description =  "是否删除，0否，1是")
        private Integer isDel;

    }

    /**
     * <p>
     * ShopProductAttr查询DTO
     * </p>
     *
     * @author car
     * @since 2025-12-04
     */
    @Data
    @Schema(description = "ShopProductAttr查询DTO")
    public static class ListQuery extends PageQuery {

    }

    /**
     * Created by IntelliJ IDEA.
     *
     * @Author car
     * @create 2026/1/19
     */
    @Data
    public static class Option {
        @Schema(description = "图片url")
        private String image;

        @Schema(description = "属性值")
        private String optionName;

        @Schema(description = "排序")
        private Integer sort;
    }

    /**
     * Created by IntelliJ IDEA.
     *
     * @Author car
     * @create 2026/1/19
     */
    @Data
    @Accessors(chain = true)
    public static class Value {
        @Schema(description = "商品id")
        private Integer productId;

        @Schema(description = "商品属性索引值 (attr_value|attr_value[|....])")
        private String sku;

        @Schema(description = "属性对应的库存")
        private Integer stock;

        @Schema(description = "属性金额")
        private BigDecimal price;

        @Schema(description = "图片")
        private String image;

        @Schema(description = "成本价")
        private BigDecimal cost;

        @Schema(description = "商品条码")
        private String barCode;

        @Schema(description = "原价")
        private BigDecimal otPrice;

        @Schema(description = "重量")
        private BigDecimal weight;

        @Schema(description = "体积")
        private BigDecimal volume;

        @Schema(description = "商品属性值")
        @JsonProperty("attr_arr")
        private List<String> attrArr = new ArrayList<>();

        @Schema(description = "商品属性值json")
        private String attrValue;

        @Schema(description = "是否默认")
        private Boolean isDefault;


    }
}
