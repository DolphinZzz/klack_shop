package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopUserRechargeActivityDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopUserRechargeActivity;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopUserRechargeActivityVO;

/**
 * <p>
 * 充值活动表 Service
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
public interface ShopUserRechargeActivityService extends IService<ShopUserRechargeActivity> {

    void create(ShopUserRechargeActivityDTO.Create dto);

    void update(ShopUserRechargeActivityDTO.Update dto);

    PageResult<ShopUserRechargeActivityVO> page(ShopUserRechargeActivityDTO.ListQuery dto);

    List<ShopUserRechargeActivityVO> list(ShopUserRechargeActivityDTO.ListQuery dto);

    List<ShopUserRechargeActivityVO> frontList();

    void remove(SelectIdsDTO dto);

    ShopUserRechargeActivityVO detail(Object id);
}
