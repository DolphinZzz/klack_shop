package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopProductCouponDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopProductCouponUser;
import com.sz.admin.shop_system.pojo.vo.ShopCouponAndUserVO;
import com.sz.core.common.entity.PageResult;

/**
 * <p>
 * 优惠券使用表 Service
 * </p>
 *
 * @author car
 * @since 2026-02-10
 */
public interface ShopProductCouponUserService extends IService<ShopProductCouponUser> {

    PageResult<ShopCouponAndUserVO> frontPage(Long userId, ShopProductCouponDTO.FrontUserList dto);
}
