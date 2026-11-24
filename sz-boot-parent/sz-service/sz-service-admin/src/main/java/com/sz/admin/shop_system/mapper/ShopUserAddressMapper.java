package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserAddressDTO;
import com.sz.admin.shop_system.pojo.po.ShopUserAddress;
import com.sz.admin.shop_system.pojo.vo.ShopUserAddressVO;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 用户地址表 Mapper 接口
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
public interface ShopUserAddressMapper extends BaseMapper<ShopUserAddress> {

    List<ShopUserAddressVO> selectAddressList(@Param("dto") ShopUserAddressDTO.ListQuery dto);

    List<Long> selectIdsByIdsAndUid(@Param("ids") List<? extends Serializable> ids, @Param("uid") Long uid);

    List<ShopUserAddress> selectActiveByUid(@Param("uid") Long uid);

    ShopUserAddress selectDefaultByUid(@Param("uid") Long uid);

    ShopUserAddress selectActiveByIdAndUid(@Param("id") Long id, @Param("uid") Long uid);

}
