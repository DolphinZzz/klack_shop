package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopFlashSaleDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopFlashSale;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleDetailVO;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleFrontVO;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleVO;
import com.sz.core.common.entity.PageResult;

import java.time.LocalDateTime;
import java.util.List;

public interface ShopFlashSaleService extends IService<ShopFlashSale> {

    PageResult<ShopFlashSaleVO> page(ShopFlashSaleDTO.ListQuery dto);

    ShopFlashSaleDetailVO detail(Long productId);

    void updateMarketing(ShopFlashSaleDTO.Update dto);

    List<ShopFlashSaleFrontVO> activeFrontList(Integer size);

    ShopFlashSaleFrontVO frontDetail(Long productId);

    ShopFlashSale getAvailableByProductId(Long productId, LocalDateTime now);
}
