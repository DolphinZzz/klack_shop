package com.sz.admin.shop_system.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopUserFootprintMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserFootprintDTO;
import com.sz.admin.shop_system.pojo.po.ShopProduct;
import com.sz.admin.shop_system.pojo.po.ShopUserFootprint;
import com.sz.admin.shop_system.pojo.vo.ShopFrontUserFootprintVO;
import com.sz.admin.shop_system.service.ShopProductService;
import com.sz.admin.shop_system.service.ShopUserFootprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopUserFootprintServiceImpl extends ServiceImpl<ShopUserFootprintMapper, ShopUserFootprint> implements ShopUserFootprintService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 20;

    private final ShopProductService lbProductService;

    @Override
    public void record(Long userId, Long productId) {
        if (userId == null || productId == null || productId <= 0) {
            return;
        }

        ShopProduct product = resolveAvailableProduct(productId);
        if (product == null) {
            return;
        }

        ShopUserFootprint footprint = mapper.selectActiveByUidAndProductId(userId, productId);
        if (footprint == null) {
            save(buildNewFootprint(userId, product));
            return;
        }
        updateById(buildRefreshFootprint(footprint.getId(), product));
    }

    @Override
    public List<ShopFrontUserFootprintVO> frontList(Long userId, ShopUserFootprintDTO.FrontList dto) {
        if (userId == null) {
            return Collections.emptyList();
        }

        int limit = resolveLimit(dto);
        List<ShopFrontUserFootprintVO> rows = mapper.selectActiveFrontListByUid(userId);
        if (rows.isEmpty()) {
            return rows;
        }
        return assembleLatestUniqueFootprints(rows, limit);
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

    private ShopUserFootprint buildNewFootprint(Long userId, ShopProduct product) {
        ShopUserFootprint footprint = new ShopUserFootprint();
        footprint.setUid(userId);
        footprint.setProductId(product.getId());
        footprint.setProductName(product.getProductName());
        footprint.setProductInfo(product.getProductInfo());
        footprint.setProductImage(product.getImage());
        footprint.setProductPrice(product.getPrice());
        footprint.setGiveIntegral(product.getGiveIntegral());
        footprint.setIsDel(0);
        footprint.setVersion(0);
        return footprint;
    }

    private ShopUserFootprint buildRefreshFootprint(Long footprintId, ShopProduct product) {
        ShopUserFootprint update = new ShopUserFootprint();
        update.setId(footprintId);
        update.setProductName(product.getProductName());
        update.setProductInfo(product.getProductInfo());
        update.setProductImage(product.getImage());
        update.setProductPrice(product.getPrice());
        update.setGiveIntegral(product.getGiveIntegral());
        return update;
    }

    private int resolveLimit(ShopUserFootprintDTO.FrontList dto) {
        int limit = dto == null || dto.getLimit() == null ? DEFAULT_LIMIT : dto.getLimit();
        if (limit < 1) {
            limit = DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private List<ShopFrontUserFootprintVO> assembleLatestUniqueFootprints(List<ShopFrontUserFootprintVO> rows, int limit) {
        Map<Long, ShopFrontUserFootprintVO> latestByProductId = new LinkedHashMap<>();
        for (ShopFrontUserFootprintVO row : rows) {
            if (row.getProductId() == null || latestByProductId.containsKey(row.getProductId())) {
                continue;
            }
            latestByProductId.put(row.getProductId(), row);
            if (latestByProductId.size() >= limit) {
                break;
            }
        }
        return latestByProductId.values().stream().toList();
    }
}
