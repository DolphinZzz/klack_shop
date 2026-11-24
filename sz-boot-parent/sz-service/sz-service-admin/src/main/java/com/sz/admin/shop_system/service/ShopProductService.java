package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopProductDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.page.ProductDetailResponse;
import com.sz.admin.shop_system.pojo.page.ProductResponse;
import com.sz.admin.shop_system.pojo.po.ShopProduct;
import com.sz.admin.shop_system.pojo.vo.ShopFrontProductCommentVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductDetailVO;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopProductVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>
 * 商品表 Service
 * </p>
 *
 * @author car
 * @since 2025-12-03
 */
public interface ShopProductService extends IService<ShopProduct> {

    void create(ShopProductDTO.Create dto);

    void update(ShopProductDTO.Update dto);

    PageResult<ShopProductVO> page(ShopProductDTO.ListQuery dto);

    List<ShopProductVO> list(ShopProductDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopProductDetailVO detail(Object id);

    void exportExcel(ShopProductDTO.ListQuery dto, HttpServletResponse response);

    PageResult<ProductResponse> getCategoryProductList(ShopProductDTO.FrontList request);

    ProductDetailResponse getDetail(Integer id, String type);

    PageResult<ShopFrontProductCommentVO> getFrontCommentPage(Integer productId, ShopProductDTO.FrontCommentPage dto);
}
