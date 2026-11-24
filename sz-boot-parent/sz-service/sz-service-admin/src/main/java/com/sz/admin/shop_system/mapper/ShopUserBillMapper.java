package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserBillDTO;
import com.sz.admin.shop_system.pojo.po.ShopUserBill;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserBillVO;
import com.sz.admin.shop_system.pojo.vo.ShopUserBillVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 用户帐单表 Mapper 接口
 * </p>
 *
 * @author sz-admin
 * @since 2025-11-26
 */
public interface ShopUserBillMapper extends BaseMapper<ShopUserBill> {

    long countById(@Param("id") Long id);

    List<ShopUserBillVO> selectBillList(@Param("dto") ShopUserBillDTO.ListQuery dto);

    BigDecimal sumPositiveNumberByUidAndCategoryAndPm(
            @Param("uid") Long uid,
            @Param("category") String category,
            @Param("pm") Integer pm
    );

    List<ShopFrontUserBillVO> selectFrontBillList(
            @Param("uid") Long uid,
            @Param("category") String category,
            @Param("tab") String tab,
            @Param("pmPay") Integer pmPay,
            @Param("pmRefund") Integer pmRefund,
            @Param("rechargeType") String rechargeType
    );

}
