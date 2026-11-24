package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopFlashSaleDTO;
import com.sz.admin.shop_system.pojo.po.ShopFlashSale;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleFrontVO;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShopFlashSaleMapper extends BaseMapper<ShopFlashSale> {

    List<ShopFlashSaleVO> selectMarketingPage(@Param("dto") ShopFlashSaleDTO.ListQuery dto);

    ShopFlashSale selectByProductId(@Param("productId") Long productId);

    List<ShopFlashSaleFrontVO> selectActiveFrontList(@Param("now") LocalDateTime now, @Param("limit") Integer limit);

    ShopFlashSaleFrontVO selectFrontDetailByProductId(@Param("productId") Long productId);

    ShopFlashSale selectAvailableByProductId(@Param("productId") Long productId, @Param("now") LocalDateTime now);
}
