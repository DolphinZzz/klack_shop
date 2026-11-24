package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserRechargeDTO;
import com.sz.admin.shop_system.pojo.po.ShopUserRecharge;
import com.sz.admin.shop_system.pojo.vo.ShopUserRechargeVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 用户充值表 Mapper 接口
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
public interface ShopUserRechargeMapper extends BaseMapper<ShopUserRecharge> {

    long countById(@Param("id") Long id);

    long countByOrderId(@Param("orderId") Long orderId);

    BigDecimal sumPositivePriceByUid(@Param("uid") Long uid);

    List<ShopUserRechargeVO> selectRechargeList(@Param("dto") ShopUserRechargeDTO.ListQuery dto);

}
