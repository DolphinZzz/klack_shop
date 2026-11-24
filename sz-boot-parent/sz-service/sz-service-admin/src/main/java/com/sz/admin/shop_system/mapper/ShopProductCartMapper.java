package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.po.ShopProductCart;
import com.sz.admin.shop_system.pojo.vo.ShopProductCartVO;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 购物车表 Mapper 接口
 * </p>
 *
 * @author car
 * @since 2026-01-26
 */
public interface ShopProductCartMapper extends BaseMapper<ShopProductCart> {

    ShopProductCart selectByUidAndProductAndSku(@Param("uid") Long uid,
                                              @Param("productId") Long productId,
                                              @Param("skuId") Long skuId);

    ShopProductCart selectActiveByIdAndUid(@Param("id") Long id, @Param("uid") Long uid);

    List<ShopProductCartVO> selectCartPageList(@Param("uid") Long uid, @Param("status") Integer status);

    int markRemovedByIdsAndUid(@Param("uid") Long uid, @Param("ids") Collection<? extends Serializable> ids);

    Integer sumActiveCartNumByUid(@Param("uid") Long uid);

    List<ShopProductCart> selectByIds(@Param("ids") Collection<Long> ids);

    List<ShopProductCart> selectByUidAndIds(@Param("uid") Long uid, @Param("ids") Collection<Long> ids);
}
