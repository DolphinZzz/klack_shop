package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.po.ShopUserIntegralBill;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserIntegralBillVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShopUserIntegralBillMapper extends BaseMapper<ShopUserIntegralBill> {

    List<ShopFrontUserIntegralBillVO> selectFrontIntegralBillList(
            @Param("uid") Long uid,
            @Param("category") String category
    );

    Integer sumPositiveNumberByUidAndCategoryAndPm(
            @Param("uid") Long uid,
            @Param("category") String category,
            @Param("pm") Integer pm
    );

}
