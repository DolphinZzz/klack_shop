package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopProductAttrMapper;
import com.sz.admin.shop_system.pojo.dto.ShopProductAttrDTO;
import com.sz.admin.shop_system.pojo.po.ShopProductAttr;
import com.sz.admin.shop_system.pojo.vo.ShopProductAttrVO;
import com.sz.admin.shop_system.service.ShopProductAttrService;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.datascope.SimpleDataScopeHelper;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 商品属性表 服务实现类
 * </p>
 *
 * @author car
 * @since 2025-12-04
 */
@Service
@RequiredArgsConstructor
public class ShopProductAttrServiceImpl extends ServiceImpl<ShopProductAttrMapper, ShopProductAttr> implements ShopProductAttrService {

    @Override
    public void create(ShopProductAttrDTO.Create dto) {
        ShopProductAttr lbProductAttr = BeanCopyUtils.copy(dto, ShopProductAttr.class);
        save(lbProductAttr);
    }

    @Override
    public void update(ShopProductAttrDTO.Update dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getId() == null || getById(dto.getId()) == null);
        ShopProductAttr lbProductAttr = BeanCopyUtils.copy(dto, ShopProductAttr.class);
        saveOrUpdate(lbProductAttr);
    }

    @Override
    public PageResult<ShopProductAttrVO> page(ShopProductAttrDTO.ListQuery dto) {
        SimpleDataScopeHelper.start(ShopProductAttr.class);
        PageUtils.toPage(dto);
        try {
            List<ShopProductAttrVO> list = mapper.selectAttrList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            PageHelper.clearPage();
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    @Override
    public List<ShopProductAttrVO> list(ShopProductAttrDTO.ListQuery dto) {
        SimpleDataScopeHelper.start(ShopProductAttr.class);
        try {
            return mapper.selectAttrList(dto);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    @Override
    public void remove(SelectIdsDTO dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getIds().isEmpty());
        removeByIds(dto.getIds());
    }

    @Override
    public ShopProductAttrVO detail(Object id) {
        ShopProductAttr lbProductAttr = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(lbProductAttr);
        return BeanCopyUtils.copy(lbProductAttr, ShopProductAttrVO.class);
    }

    @Override
    public List<ShopProductAttr> getListByProductIdAndType(Long id, Integer type) {
        return mapper.selectByProductIdAndType(id, type);
    }
}
