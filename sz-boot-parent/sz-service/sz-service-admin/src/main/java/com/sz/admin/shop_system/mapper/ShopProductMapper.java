package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopProductDTO;
import com.sz.admin.shop_system.pojo.page.ProductResponse;
import com.sz.admin.shop_system.pojo.po.ShopProduct;
import com.sz.admin.shop_system.pojo.vo.ShopFrontProductCommentVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 商品表 Mapper 接口
 * </p>
 *
 * @author car
 * @since 2025-12-03
 */
public interface ShopProductMapper extends BaseMapper<ShopProduct> {

    List<ShopProductVO> selectProductList(@Param("dto") ShopProductDTO.ListQuery dto);

    List<ProductResponse> selectFrontCategoryProductList(@Param("categoryIdList") List<Integer> categoryIdList,
                                                         @Param("keyword") String keyword,
                                                         @Param("minPrice") BigDecimal minPrice,
                                                         @Param("maxPrice") BigDecimal maxPrice,
                                                         @Param("salesOrder") String salesOrder,
                                                         @Param("priceOrder") String priceOrder);

    List<ShopFrontProductCommentVO> selectFrontCommentList(@Param("productId") Integer productId);

    List<ShopProduct> selectNewRecommendProducts(@Param("limit") Integer limit);

    List<ShopProduct> selectBestProductsByBrowse(@Param("limit") Integer limit);

    List<ShopProduct> selectNonBestProductsByBrowse(@Param("limit") Integer limit);

    List<ShopProduct> selectHomeShowProducts(@Param("limit") Integer limit);

    int updateStockOptimistic(@Param("id") Long id,
                              @Param("newStock") Integer newStock,
                              @Param("newVersion") Integer newVersion,
                              @Param("expectedVersion") Integer expectedVersion);

    List<ShopProduct> selectAgentRecommendProducts(@Param("sort") String sort,
                                                   @Param("categoryIdList") List<Integer> categoryIdList,
                                                   @Param("limit") Integer limit);

    List<ShopProduct> selectAgentSearchProducts(@Param("keyword") String keyword,
                                                @Param("categoryIdList") List<Integer> categoryIdList,
                                                @Param("minPrice") BigDecimal minPrice,
                                                @Param("maxPrice") BigDecimal maxPrice,
                                                @Param("limit") Integer limit);
}
