package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopProductCouponDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopProductCoupon;
import com.sz.admin.shop_system.pojo.vo.ShopCouponAndProductsVO;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopProductCouponVO;

/**
 * <p>
 * 商品优惠券表 Service
 * </p>
 *
 * @author car
 * @since 2026-01-30
 */
public interface ShopProductCouponService extends IService<ShopProductCoupon> {

    void create(ShopProductCouponDTO.Create dto);

    void update(ShopProductCouponDTO.Update dto);

    PageResult<ShopCouponAndProductsVO> page(ShopProductCouponDTO.ListQuery dto);

    PageResult<ShopProductCouponVO> frontList(ShopProductCouponDTO.ListQuery dto);

    String grabCoupon(Long couponId);

    List<ShopProductCouponVO> list(ShopProductCouponDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopProductCouponVO detail(Object id);
}
