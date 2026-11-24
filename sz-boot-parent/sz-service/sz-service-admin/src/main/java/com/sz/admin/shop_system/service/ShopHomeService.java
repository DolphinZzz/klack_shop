package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.vo.ShopHomeIndexVO;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleFrontVO;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewBookVO;

import java.util.List;

/**
 * 首页聚合 Service
 *
 * @author car
 * @since 2026-02-26
 */
public interface ShopHomeService {

    /**
     * PC端首页聚合数据
     *
     * @return 首页数据
     */
    ShopHomeIndexVO frontIndex();

    /**
     * 新品推荐（按创建时间倒序）
     *
     * @param size 数量
     * @return 新品列表
     */
    List<ShopHomeNewBookVO> newRecommend(Integer size);

    /**
     * 猜你喜欢（按浏览量倒序）
     *
     * @param size 数量
     * @return 商品列表
     */
    List<ShopHomeNewBookVO> guessLike(Integer size);

    /**
     * 首页秒杀专区
     *
     * @param size 数量
     * @return 秒杀商品
     */
    List<ShopFlashSaleFrontVO> flashSale(Integer size);
}
