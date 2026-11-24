package com.sz.admin.shop_system.pojo.vo;

import com.sz.excel.annotation.DictFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author car
 * @create 2026/1/16
 */
@Data
@Schema(description = "商品详情VO")
public class ShopProductDetailVO {
    @Schema(description = "商品id")
    private Long id;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "分类id")
    private Integer cateId;

    @Schema(description = "商品介绍")
    private String productInfo;

    @Schema(description = "商品价格")
    private BigDecimal price;

    @Schema(description = "快递公司名")
    private String expressName;

    @Schema(description = "快递模板id")
    private Integer expressId;

    @Schema(description = "邮费")
    private BigDecimal postage;

    @Schema(description = "赠送积分")
    private BigDecimal giveIntegral;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "库存")
    private Integer stock;

    @DictFormat(dictType = "is_show")
    @Schema(description = "状态")
    private Integer isShow;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "商品单位")
    private String unitName;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "商品简介")
    private String desc;

    @Schema(description = "封面图")
    private String image;

    @Schema(description = "轮播图")
    private String sliderImage;

    @Schema(description = "商品属性")
    private List<ShopProductAttrVO> attrList;

    @Schema(description = "商品属性值")
    private List<ShopProductAttrValueVO> attrValueList;
}
