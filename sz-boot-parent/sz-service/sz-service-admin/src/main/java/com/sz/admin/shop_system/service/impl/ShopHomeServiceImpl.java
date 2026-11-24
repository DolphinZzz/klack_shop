package com.sz.admin.shop_system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.sz.admin.shop_system.mapper.ShopProductMapper;
import com.sz.admin.shop_system.pojo.po.ShopProduct;
import com.sz.admin.shop_system.pojo.vo.ShopCategoryVO;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleFrontVO;
import com.sz.admin.shop_system.pojo.vo.ShopHomeIndexVO;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewBookVO;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewsVO;
import com.sz.admin.shop_system.pojo.vo.ShopUserVO;
import com.sz.admin.shop_system.service.ShopCategoryService;
import com.sz.admin.shop_system.service.ShopFlashSaleService;
import com.sz.admin.shop_system.service.ShopHomeNewsService;
import com.sz.admin.shop_system.service.ShopHomeService;
import com.sz.admin.shop_system.service.ShopUserService;
import com.sz.security.util.StpUserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 首页聚合 Service 实现
 *
 * @author car
 * @since 2026-02-26
 */
@Service
@RequiredArgsConstructor
public class ShopHomeServiceImpl implements ShopHomeService {

    private static final int MAX_CATEGORY_SIZE = 8;
    private static final int MAX_BANNER_SIZE = 5;
    private static final int MAX_SHOP_SIZE = 4;
    private static final int MAX_SHOP_PRODUCTS_SIZE = 2;
    private static final int MAX_NOTICE_SIZE = 3;
    private static final int DEFAULT_RECOMMEND_SIZE = 8;
    private static final int MAX_NEW_RECOMMEND_SIZE = 12;
    private static final int MAX_SHOW_PRODUCTS_SIZE = 80;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ShopCategoryService shopCategoryService;
    private final ShopHomeNewsService shopHomeNewsService;
    private final ShopFlashSaleService shopFlashSaleService;
    private final ShopProductMapper shopProductMapper;
    private final ShopUserService shopUserService;

    @Override
    public ShopHomeIndexVO frontIndex() {
        List<ShopCategoryVO> categoryTree = shopCategoryService.cateChildrenList();
        List<ShopProduct> showProducts = loadHomeShowProducts();
        return assembleHomeIndex(categoryTree, showProducts);
    }

    @Override
    public List<ShopHomeNewBookVO> newRecommend(Integer size) {
        int limit = resolveRecommendLimit(size);
        List<ShopProduct> products = shopProductMapper.selectNewRecommendProducts(limit);
        return buildHomeProductList(products);
    }

    @Override
    public List<ShopHomeNewBookVO> guessLike(Integer size) {
        int limit = resolveRecommendLimit(size);
        List<ShopProduct> products = loadGuessLikeProducts(limit);
        return buildHomeProductList(products);
    }

    @Override
    public List<ShopFlashSaleFrontVO> flashSale(Integer size) {
        return shopFlashSaleService.activeFrontList(size);
    }

    private int resolveRecommendLimit(Integer size) {
        return (size == null || size <= 0) ? DEFAULT_RECOMMEND_SIZE : Math.min(size, MAX_NEW_RECOMMEND_SIZE);
    }

    private List<ShopProduct> loadHomeShowProducts() {
        return shopProductMapper.selectHomeShowProducts(MAX_SHOW_PRODUCTS_SIZE);
    }

    private List<ShopProduct> loadGuessLikeProducts(int limit) {
        List<ShopProduct> products = new ArrayList<>(shopProductMapper.selectBestProductsByBrowse(limit));
        if (products.size() < limit) {
            int remain = limit - products.size();
            products.addAll(shopProductMapper.selectNonBestProductsByBrowse(remain));
        }
        return products;
    }

    private ShopHomeIndexVO assembleHomeIndex(List<ShopCategoryVO> categoryTree, List<ShopProduct> showProducts) {
        ShopHomeIndexVO result = new ShopHomeIndexVO();
        result.setNavList(buildNavList());
        result.setCategoryList(buildCategoryList(categoryTree));
        result.setBannerList(buildBannerList(showProducts));
        result.setNoticeList(buildNoticeList());
        result.setUserCard(buildUserCard());
        result.setFeatured(buildFeatured(showProducts));
        result.setBrandShopList(buildBrandShopList(showProducts, categoryTree));
        return result;
    }

    private List<ShopHomeNewBookVO> buildHomeProductList(List<ShopProduct> products) {
        if (CollUtil.isEmpty(products)) {
            return Collections.emptyList();
        }
        return products.stream().map(product -> {
            ShopHomeNewBookVO vo = new ShopHomeNewBookVO();
            vo.setId(product.getId());
            vo.setProductName(product.getProductName());
            vo.setImage(resolveCoverImage(product));
            vo.setPrice(product.getPrice() == null ? BigDecimal.ZERO : product.getPrice());
            vo.setSales(product.getSales() == null ? 0 : product.getSales());
            vo.setBrowse(product.getBrowse() == null ? 0 : product.getBrowse());
            vo.setIsBest(product.getIsBest() == null ? 0 : product.getIsBest());
            if (product.getCreateTime() != null) {
                vo.setCreateTime(product.getCreateTime().format(DATE_TIME_FORMATTER));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private List<ShopHomeIndexVO.HomeNavVO> buildNavList() {
        List<ShopHomeIndexVO.HomeNavVO> navList = new ArrayList<>();
        navList.add(buildNav("flash", "限时秒杀", "/flash"));
        navList.add(buildNav("brand", "品牌好店", "/brand"));
        navList.add(buildNav("coupon", "领券中心", "/coupon"));
        navList.add(buildNav("order", "订单中心", "/order"));
        navList.add(buildNav("notice", "资讯信息", "/notice"));
        navList.add(buildNav("mine-coupon", "我的优惠券", "/my/coupon"));
        return navList;
    }

    private ShopHomeIndexVO.HomeNavVO buildNav(String key, String name, String path) {
        ShopHomeIndexVO.HomeNavVO nav = new ShopHomeIndexVO.HomeNavVO();
        nav.setKey(key);
        nav.setName(name);
        nav.setPath(path);
        return nav;
    }

    private List<ShopHomeIndexVO.HomeCategoryVO> buildCategoryList(List<ShopCategoryVO> categoryTree) {
        if (CollUtil.isEmpty(categoryTree)) {
            return Collections.emptyList();
        }
        return categoryTree.stream()
                .limit(MAX_CATEGORY_SIZE)
                .map(category -> {
                    ShopHomeIndexVO.HomeCategoryVO item = new ShopHomeIndexVO.HomeCategoryVO();
                    item.setId(category.getId());
                    item.setName(category.getCateName());
                    List<String> children = CollUtil.isEmpty(category.getChildren())
                            ? Collections.emptyList()
                            : category.getChildren().stream()
                            .map(ShopCategoryVO::getCateName)
                            .filter(StrUtil::isNotBlank)
                            .limit(2)
                            .collect(Collectors.toList());
                    item.setChildren(children);
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<ShopHomeIndexVO.HomeBannerVO> buildBannerList(List<ShopProduct> products) {
        if (CollUtil.isEmpty(products)) {
            return Collections.emptyList();
        }
        return products.stream()
                .limit(MAX_BANNER_SIZE)
                .map(product -> {
                    ShopHomeIndexVO.HomeBannerVO banner = new ShopHomeIndexVO.HomeBannerVO();
                    banner.setId(Math.toIntExact(product.getId()));
                    banner.setTitle(product.getProductName());
                    banner.setImage(resolveCoverImage(product));
                    return banner;
                })
                .collect(Collectors.toList());
    }

    private List<ShopHomeIndexVO.HomeNoticeVO> buildNoticeList() {
        List<ShopHomeNewsVO> newsList = shopHomeNewsService.frontList(MAX_NOTICE_SIZE);
        if (CollUtil.isEmpty(newsList)) {
            List<ShopHomeIndexVO.HomeNoticeVO> notices = new ArrayList<>();
            notices.add(buildNotice(1, "欢迎来到商城首页"));
            notices.add(buildNotice(2, "商品活动正在更新中"));
            notices.add(buildNotice(3, "新版PC页面已上线"));
            return notices;
        }
        return newsList.stream().map(news -> {
            ShopHomeIndexVO.HomeNoticeVO notice = new ShopHomeIndexVO.HomeNoticeVO();
            notice.setId(news.getId() == null ? 0 : Math.toIntExact(news.getId()));
            notice.setTitle(news.getTitle());
            notice.setTagName(StrUtil.blankToDefault(news.getTagName(), "资讯"));
            notice.setSummary(news.getSummary());
            notice.setCoverImage(news.getCoverImage());
            notice.setLink(news.getLink());
            if (news.getPublishTime() != null) {
                notice.setPublishTime(news.getPublishTime().format(DATE_TIME_FORMATTER));
            }
            return notice;
        }).collect(Collectors.toList());
    }

    private ShopHomeIndexVO.HomeNoticeVO buildNotice(Integer id, String title) {
        ShopHomeIndexVO.HomeNoticeVO notice = new ShopHomeIndexVO.HomeNoticeVO();
        notice.setId(id);
        notice.setTitle(title);
        notice.setTagName("资讯");
        return notice;
    }

    private ShopHomeIndexVO.HomeUserCardVO buildUserCard() {
        ShopHomeIndexVO.HomeUserCardVO userCard = new ShopHomeIndexVO.HomeUserCardVO();
        userCard.setFavorites(0);
        userCard.setFollows(0);
        userCard.setFootprints(0);

        if (!StpUserUtil.isLogin()) {
            userCard.setLoggedIn(false);
            userCard.setNickname("Hi~你好");
            return userCard;
        }

        try {
            long uid = StpUserUtil.getLoginIdAsLong();
            ShopUserVO user = shopUserService.detail(uid);
            userCard.setLoggedIn(true);
            userCard.setNickname(StrUtil.blankToDefault(user.getNickname(), user.getAccount()));
            userCard.setAvatar(user.getAvatar());
        } catch (Exception ex) {
            userCard.setLoggedIn(false);
            userCard.setNickname("Hi~你好");
        }
        return userCard;
    }

    private ShopHomeIndexVO.HomeFeaturedVO buildFeatured(List<ShopProduct> products) {
        ShopHomeIndexVO.HomeFeaturedVO featured = new ShopHomeIndexVO.HomeFeaturedVO();
        if (CollUtil.isEmpty(products)) {
            featured.setId(0);
            featured.setTitle("品牌好店");
            featured.setSubTitle("品质好物持续上新");
            featured.setImage("");
            return featured;
        }

        ShopProduct product = products.get(0);
        featured.setId(Math.toIntExact(product.getId()));
        featured.setTitle(StrUtil.blankToDefault(product.getProductName(), "品牌好店"));
        featured.setSubTitle("新品推荐");
        featured.setImage(resolveCoverImage(product));
        return featured;
    }

    private List<ShopHomeIndexVO.HomeShopVO> buildBrandShopList(List<ShopProduct> products, List<ShopCategoryVO> categoryTree) {
        if (CollUtil.isEmpty(products)) {
            return Collections.emptyList();
        }

        Map<Integer, String> categoryNameMap = buildCategoryNameMap(categoryTree);
        Map<Integer, List<ShopProduct>> grouped = products.stream()
                .filter(item -> Objects.nonNull(item.getCateId()))
                .collect(Collectors.groupingBy(
                        ShopProduct::getCateId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Integer, List<ShopProduct>> e) ->
                        e.getValue().stream().map(ShopProduct::getSales).filter(Objects::nonNull).mapToInt(Integer::intValue).sum()
                ).reversed())
                .limit(MAX_SHOP_SIZE)
                .map(entry -> buildBrandShop(entry.getKey(), entry.getValue(), categoryNameMap))
                .collect(Collectors.toList());
    }

    private Map<Integer, String> buildCategoryNameMap(List<ShopCategoryVO> categoryTree) {
        Map<Integer, String> categoryNameMap = new LinkedHashMap<>();
        if (CollUtil.isEmpty(categoryTree)) {
            return categoryNameMap;
        }
        for (ShopCategoryVO root : categoryTree) {
            categoryNameMap.put(root.getId(), root.getCateName());
            if (CollUtil.isNotEmpty(root.getChildren())) {
                for (ShopCategoryVO child : root.getChildren()) {
                    categoryNameMap.put(child.getId(), child.getCateName());
                }
            }
        }
        return categoryNameMap;
    }

    private ShopHomeIndexVO.HomeShopVO buildBrandShop(Integer cateId,
                                                    List<ShopProduct> shopProductEntities,
                                                    Map<Integer, String> categoryNameMap) {
        ShopHomeIndexVO.HomeShopVO shop = new ShopHomeIndexVO.HomeShopVO();
        shop.setId(cateId);
        shop.setName(StrUtil.blankToDefault(categoryNameMap.get(cateId), "精选店铺"));
        shop.setTag("自营");

        List<ShopHomeIndexVO.HomeShopProductVO> shopProducts = shopProductEntities.stream()
                .limit(MAX_SHOP_PRODUCTS_SIZE)
                .map(product -> {
                    ShopHomeIndexVO.HomeShopProductVO productVO = new ShopHomeIndexVO.HomeShopProductVO();
                    productVO.setId(Math.toIntExact(product.getId()));
                    productVO.setName(product.getProductName());
                    productVO.setImage(resolveCoverImage(product));
                    productVO.setPrice(product.getPrice() == null ? BigDecimal.ZERO : product.getPrice());
                    return productVO;
                })
                .collect(Collectors.toList());
        shop.setProducts(shopProducts);
        return shop;
    }

    private String resolveCoverImage(ShopProduct product) {
        if (StrUtil.isNotBlank(product.getImage())) {
            return product.getImage();
        }
        if (StrUtil.isBlank(product.getSliderImage())) {
            return "";
        }
        String[] split = product.getSliderImage().split(",");
        return split.length > 0 ? split[0] : "";
    }
}
