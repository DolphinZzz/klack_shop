package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopOrderDTO;
import com.sz.admin.shop_system.pojo.po.ShopOrder;
import com.sz.admin.shop_system.pojo.vo.ShopFrontProductCommentVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductCartAndSkuVO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 订单表 Mapper 接口
 * </p>
 *
 * @author car
 * @since 2026-01-28
 */
public interface ShopOrderMapper extends BaseMapper<ShopOrder> {

    /**
     * 按订单号和用户ID查询单笔订单
     */
    ShopOrder selectByOrderNoAndUid(@Param("orderId") String orderId, @Param("uid") Long uid);

    /**
     * 按订单号和用户ID查询未删除订单
     */
    ShopOrder selectActiveByOrderNoAndUid(@Param("orderId") String orderId, @Param("uid") Long uid);

    /**
     * 按订单号查询单笔订单
     */
    ShopOrder selectByOrderNo(@Param("orderId") String orderId);

    /**
     * 查询用户所有未删除订单（按创建时间倒序）
     */
    List<ShopOrder> selectActiveListByUid(@Param("uid") Long uid);

    /**
     * 管理端订单列表查询（配合 PageHelper 分页）
     */
    List<ShopOrderVO> selectAdminOrderList(@Param("dto") ShopOrderDTO.ListQuery dto);

    /**
     * 原子化关闭超时未支付订单
     */
    int expirePendingOrder(@Param("id") Long id,
                           @Param("paid") Integer paid,
                           @Param("status") Integer status,
                           @Param("pendingPaid") Integer pendingPaid);

    /**
     * PC端用户评论列表查询（配合 PageHelper 分页）
     */
    List<ShopFrontProductCommentVO> selectFrontCommentList(@Param("uid") Long uid,
                                                         @Param("keyword") String keyword);

    /**
     * 按购物车ID集合查询订单关联的商品与SKU明细（支持按用户ID、购物车状态过滤）
     */
    List<ShopProductCartAndSkuVO> selectOrderCartWithSku(@Param("cartIds") Collection<? extends Number> cartIds,
                                                       @Param("uid") Long uid,
                                                       @Param("status") Integer status);
}
