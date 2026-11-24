package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.po.ShopProductCouponUser;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 优惠券使用表 Mapper 接口
 * </p>
 *
 * @author car
 * @since 2026-02-10
 */
public interface ShopProductCouponUserMapper extends BaseMapper<ShopProductCouponUser> {

    long countByCidAndUid(@Param("cid") Long cid, @Param("uid") Long uid);

    List<Long> selectUidsByCid(@Param("cid") Long cid);

    List<ShopProductCouponUser> selectByUidOrderByCreateTimeDesc(@Param("uid") Long uid);

    ShopProductCouponUser selectAvailableByIdAndUid(@Param("id") Long id, @Param("uid") Long uid);

    int markUsedByIdAndUid(@Param("id") Long id, @Param("uid") Long uid, @Param("useTime") LocalDateTime useTime);
}
