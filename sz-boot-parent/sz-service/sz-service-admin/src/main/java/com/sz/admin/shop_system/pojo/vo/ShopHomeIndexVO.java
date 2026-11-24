package com.sz.admin.shop_system.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * PC首页聚合返回对象
 *
 * @author car
 * @since 2026-02-26
 */
@Data
@Schema(description = "PC首页聚合返回对象")
public class ShopHomeIndexVO implements Serializable {

    @Schema(description = "导航栏")
    private List<HomeNavVO> navList;

    @Schema(description = "左侧分类")
    private List<HomeCategoryVO> categoryList;

    @Schema(description = "轮播图")
    private List<HomeBannerVO> bannerList;

    @Schema(description = "用户卡片")
    private HomeUserCardVO userCard;

    @Schema(description = "资讯")
    private List<HomeNoticeVO> noticeList;

    @Schema(description = "左下角推荐大图")
    private HomeFeaturedVO featured;

    @Schema(description = "品牌好店")
    private List<HomeShopVO> brandShopList;

    @Data
    @Schema(description = "首页导航")
    public static class HomeNavVO {
        private String key;
        private String name;
        private String path;
    }

    @Data
    @Schema(description = "分类")
    public static class HomeCategoryVO {
        private Integer id;
        private String name;
        private List<String> children;
    }

    @Data
    @Schema(description = "轮播项")
    public static class HomeBannerVO {
        private Integer id;
        private String title;
        private String image;
    }

    @Data
    @Schema(description = "用户卡片")
    public static class HomeUserCardVO {
        private Boolean loggedIn;
        private String nickname;
        private String avatar;
        private Integer favorites;
        private Integer follows;
        private Integer footprints;
    }

    @Data
    @Schema(description = "资讯项")
    public static class HomeNoticeVO {
        private Integer id;
        private String title;
        private String tagName;
        private String summary;
        private String coverImage;
        private String link;
        private String publishTime;
    }

    @Data
    @Schema(description = "推荐大卡片")
    public static class HomeFeaturedVO {
        private Integer id;
        private String title;
        private String subTitle;
        private String image;
    }

    @Data
    @Schema(description = "品牌店铺")
    public static class HomeShopVO {
        private Integer id;
        private String name;
        private String tag;
        private List<HomeShopProductVO> products;
    }

    @Data
    @Schema(description = "店铺商品")
    public static class HomeShopProductVO {
        private Integer id;
        private String name;
        private String image;
        private BigDecimal price;
    }
}
