package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserDTO;
import com.sz.admin.shop_system.pojo.po.ShopUser;
import com.sz.admin.shop_system.pojo.vo.ShopUserVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author car
 * @since 2025-11-24
 */
public interface ShopUserMapper extends BaseMapper<ShopUser> {

    long countByUid(@Param("uid") Long uid);

    long countByAccount(@Param("account") String account, @Param("excludeUid") Long excludeUid);

    long countByCardId(@Param("cardId") String cardId, @Param("excludeUid") Long excludeUid);

    long countByPhone(@Param("phone") String phone, @Param("excludeUid") Long excludeUid);

    ShopUser selectOneByAccount(@Param("account") String account);

    ShopUser selectOneByPhone(@Param("phone") String phone);

    List<ShopUserVO> selectUserList(@Param("dto") ShopUserDTO.ListQuery dto);

}
