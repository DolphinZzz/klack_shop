package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopUserFavoriteMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserFavoriteDTO;
import com.sz.admin.shop_system.pojo.po.ShopProduct;
import com.sz.admin.shop_system.pojo.po.ShopUserFavorite;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserFavoriteStatusVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserFavoriteVO;
import com.sz.admin.shop_system.service.ShopProductService;
import com.sz.admin.shop_system.service.ShopUserFavoriteService;
import com.sz.core.common.entity.PageQuery;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopUserFavoriteServiceImpl extends ServiceImpl<ShopUserFavoriteMapper, ShopUserFavorite> implements ShopUserFavoriteService {

    private final ShopProductService lbProductService;

    @Override
    public void create(Long userId, Long productId) {
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);
        CommonResponseEnum.INVALID.assertNull(productId);

        ShopProduct product = resolveAvailableProduct(productId);
        CommonResponseEnum.INVALID.message("商品不存在或已下架").assertNull(product);

        ShopUserFavorite exists = mapper.selectOneByUidAndProductId(userId, productId);
        if (exists == null) {
            save(buildNewFavorite(userId, product));
            return;
        }
        updateById(buildReactivateFavorite(exists.getId(), product));
    }

    @Override
    public PageResult<ShopFrontUserFavoriteVO> page(Long userId, ShopUserFavoriteDTO.FrontList dto) {
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);

        PageQuery pageQuery = dto == null ? new PageQuery() : dto;
        PageUtils.toPage(pageQuery);
        try {
            List<ShopFrontUserFavoriteVO> list = mapper.selectFrontFavoriteList(userId);
            return PageUtils.getPageResult(list);
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public void remove(SelectIdsDTO dto, Long userId) {
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);
        CommonResponseEnum.INVALID_ID.assertTrue(dto == null || dto.getIds().isEmpty());
        mapper.softDeleteByUidAndIds(userId, dto.getIds());
    }

    @Override
    public void removeByProductId(Long userId, Long productId) {
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);
        CommonResponseEnum.INVALID.assertNull(productId);
        mapper.softDeleteByUidAndProductId(userId, productId);
    }

    @Override
    public ShopFrontUserFavoriteStatusVO status(Long userId, Long productId) {
        if (userId == null || productId == null || productId <= 0) {
            return assembleFavoriteStatus(null);
        }
        ShopUserFavorite favorite = mapper.selectActiveByUidAndProductId(userId, productId);
        return assembleFavoriteStatus(favorite);
    }

    @Override
    public Integer count(Long userId) {
        if (userId == null) {
            return 0;
        }
        return Math.toIntExact(mapper.countActiveByUid(userId));
    }

    private ShopProduct resolveAvailableProduct(Long productId) {
        ShopProduct product = lbProductService.getById(productId);
        if (product != null
                && Integer.valueOf(0).equals(product.getIsDel())
                && Integer.valueOf(1).equals(product.getIsShow())) {
            return product;
        }
        return null;
    }

    private ShopUserFavorite buildNewFavorite(Long userId, ShopProduct product) {
        ShopUserFavorite favorite = new ShopUserFavorite();
        favorite.setUid(userId);
        favorite.setProductId(product.getId());
        favorite.setProductName(product.getProductName());
        favorite.setProductInfo(product.getProductInfo());
        favorite.setProductImage(product.getImage());
        favorite.setProductPrice(product.getPrice());
        favorite.setGiveIntegral(product.getGiveIntegral());
        favorite.setIsDel(0);
        favorite.setVersion(0);
        return favorite;
    }

    private ShopUserFavorite buildReactivateFavorite(Long favoriteId, ShopProduct product) {
        ShopUserFavorite update = new ShopUserFavorite();
        update.setId(favoriteId);
        update.setProductName(product.getProductName());
        update.setProductInfo(product.getProductInfo());
        update.setProductImage(product.getImage());
        update.setProductPrice(product.getPrice());
        update.setGiveIntegral(product.getGiveIntegral());
        update.setIsDel(0);
        return update;
    }

    private ShopFrontUserFavoriteStatusVO assembleFavoriteStatus(ShopUserFavorite favorite) {
        ShopFrontUserFavoriteStatusVO statusVO = new ShopFrontUserFavoriteStatusVO();
        if (favorite == null) {
            statusVO.setCollected(false);
            return statusVO;
        }
        statusVO.setCollected(true);
        statusVO.setFavoriteId(favorite.getId());
        return statusVO;
    }
}
