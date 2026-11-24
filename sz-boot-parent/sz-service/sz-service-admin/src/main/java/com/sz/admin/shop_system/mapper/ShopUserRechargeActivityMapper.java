package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserRechargeActivityDTO;
import com.sz.admin.shop_system.pojo.po.ShopUserRechargeActivity;
import com.sz.admin.shop_system.pojo.vo.ShopUserRechargeActivityVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 充值活动表 Mapper 接口
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
public interface ShopUserRechargeActivityMapper extends BaseMapper<ShopUserRechargeActivity> {

    long countById(@Param("id") Long id);

    ShopUserRechargeActivity selectActiveById(@Param("id") Long id);

    List<ShopUserRechargeActivityVO> selectActivityList(@Param("dto") ShopUserRechargeActivityDTO.ListQuery dto);

    List<ShopUserRechargeActivityVO> selectFrontActivityList();

}
