package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopProductCouponDTO;
import com.sz.admin.shop_system.pojo.po.ShopProductCoupon;
import com.sz.admin.shop_system.pojo.vo.ShopCouponAndProductsVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductCouponVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 商品优惠券表 Mapper 接口
 * </p>
 *
 * @author car
 * @since 2026-01-30
 */
public interface ShopProductCouponMapper extends BaseMapper<ShopProductCoupon> {

    List<ShopCouponAndProductsVO> selectAdminPageList(@Param("dto") ShopProductCouponDTO.ListQuery dto);

    List<ShopProductCouponVO> selectFrontList(@Param("dto") ShopProductCouponDTO.ListQuery dto,
                                            @Param("userId") Long userId,
                                            @Param("now") LocalDateTime now);

    List<ShopProductCouponVO> selectCouponList(@Param("dto") ShopProductCouponDTO.ListQuery dto);

    int deductLastTotalOptimistically(@Param("couponId") Long couponId,
                                      @Param("expectedLastTotal") Integer expectedLastTotal);

    int deductLastTotalAtomic(@Param("couponId") Long couponId);

    List<ShopProductCoupon> selectActiveGrabbableCoupons();

    List<ShopProductCoupon> selectByIds(@Param("ids") Collection<Long> ids);
}
