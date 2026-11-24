package com.sz.admin.shop_system.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopProductAttrValueMapper;
import com.sz.admin.shop_system.pojo.po.ShopProductAttrValue;
import com.sz.admin.shop_system.service.ShopProductAttrValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 商品属性值表 服务实现类
 * </p>
 *
 * @author car
 * @since 2025-12-04
 */
@Service
@RequiredArgsConstructor
public class ShopProductAttrValueServiceImpl extends ServiceImpl<ShopProductAttrValueMapper, ShopProductAttrValue> implements ShopProductAttrValueService {

    @Override
    public List<ShopProductAttrValue> getListByProductId(Long id) {
        return mapper.selectActiveByProductId(id);
    }
}
