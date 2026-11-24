package com.sz.admin.shop_system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopHomeNewsCategoryMapper;
import com.sz.admin.shop_system.pojo.dto.ShopHomeNewsCategoryDTO;
import com.sz.admin.shop_system.pojo.po.ShopHomeNewsCategory;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewsCategoryVO;
import com.sz.admin.shop_system.service.ShopHomeNewsCategoryService;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.datascope.SimpleDataScopeHelper;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 资讯分类 Service 实现
 *
 * @author car
 * @since 2026-03-28
 */
@Service
@RequiredArgsConstructor
public class ShopHomeNewsCategoryServiceImpl extends ServiceImpl<ShopHomeNewsCategoryMapper, ShopHomeNewsCategory> implements ShopHomeNewsCategoryService {

    @Override
    public void create(ShopHomeNewsCategoryDTO.Create dto) {
        ShopHomeNewsCategory category = buildCreateEntity(dto);
        save(category);
    }

    @Override
    public void update(ShopHomeNewsCategoryDTO.Update dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getId() == null || getById(dto.getId()) == null);
        ShopHomeNewsCategory category = BeanCopyUtils.copy(dto, ShopHomeNewsCategory.class);
        saveOrUpdate(category);
    }

    @Override
    public PageResult<ShopHomeNewsCategoryVO> page(ShopHomeNewsCategoryDTO.ListQuery dto) {
        PageUtils.toPage(dto);
        try {
            SimpleDataScopeHelper.start(ShopHomeNewsCategory.class);
            List<ShopHomeNewsCategoryVO> list = mapper.selectCategoryList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ShopHomeNewsCategoryVO> list(ShopHomeNewsCategoryDTO.ListQuery dto) {
        try {
            SimpleDataScopeHelper.start(ShopHomeNewsCategory.class);
            return mapper.selectCategoryList(dto);
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
    public ShopHomeNewsCategoryVO detail(Object id) {
        ShopHomeNewsCategory category = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(category);
        return BeanCopyUtils.copy(category, ShopHomeNewsCategoryVO.class);
    }

    @Override
    public List<ShopHomeNewsCategoryVO> frontList() {
        List<ShopHomeNewsCategoryVO> list = mapper.selectFrontCategoryList();
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list;
    }

    private ShopHomeNewsCategory buildCreateEntity(ShopHomeNewsCategoryDTO.Create dto) {
        ShopHomeNewsCategory category = BeanCopyUtils.copy(dto, ShopHomeNewsCategory.class);
        if (category.getStatus() == null) {
            category.setStatus(0);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getIsDel() == null) {
            category.setIsDel(0);
        }
        return category;
    }
}
