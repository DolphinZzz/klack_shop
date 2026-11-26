package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopOrderAfterSaleDTO;
import com.sz.admin.shop_system.pojo.po.ShopOrderAfterSale;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderAfterSaleVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderAfterSaleVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单售后 Mapper 接口
 */
public interface ShopOrderAfterSaleMapper extends BaseMapper<ShopOrderAfterSale> {

    /**
     * 按订单ID查询有效售后单
     */
    ShopOrderAfterSale selectActiveByOrderId(@Param("orderId") Long orderId);

    /**
     * 按售后单ID和用户ID查询有效售后单
     */
    ShopOrderAfterSale selectActiveByIdAndUid(@Param("id") Long id, @Param("uid") Long uid);

    /**
     * 按售后单ID查询有效售后单
     */
    ShopOrderAfterSale selectActiveById(@Param("id") Long id);

    /**
     * 客户端售后分页查询（配合 PageHelper）
     */
    List<ShopFrontOrderAfterSaleVO> selectFrontPageList(@Param("uid") Long uid,
                                                      @Param("dto") ShopOrderAfterSaleDTO.FrontList dto);

    /**
     * 管理端售后分页查询（配合 PageHelper）
     */
    List<ShopOrderAfterSaleVO> selectAdminPageList(@Param("dto") ShopOrderAfterSaleDTO.ListQuery dto);

    /**
     * Agent 内网查询用户售后单列表
     */
    List<ShopOrderAfterSale> selectAgentListByUid(@Param("uid") Long uid,
                                                  @Param("orderNo") String orderNo,
                                                  @Param("serviceNo") String serviceNo,
                                                  @Param("limit") Integer limit);
}
