package com.sz.admin.shop_system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopHomeBannerMapper;
import com.sz.admin.shop_system.pojo.dto.ShopHomeBannerDTO;
import com.sz.admin.shop_system.pojo.po.ShopHomeBanner;
import com.sz.admin.shop_system.pojo.vo.ShopHomeBannerVO;
import com.sz.admin.shop_system.service.ShopHomeBannerService;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.datascope.SimpleDataScopeHelper;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 首页轮播 Service 实现
 *
 * @author car
 * @since 2026-02-26
 */
@Service
@RequiredArgsConstructor
public class ShopHomeBannerServiceImpl extends ServiceImpl<ShopHomeBannerMapper, ShopHomeBanner> implements ShopHomeBannerService {

    private static final int MAX_FRONT_SIZE = 10;
    private static final int DEFAULT_FRONT_SIZE = 5;

    @Override
    public void create(ShopHomeBannerDTO.Create dto) {
        ShopHomeBanner banner = buildCreateEntity(dto);
        save(banner);
    }

    @Override
    public void update(ShopHomeBannerDTO.Update dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getId() == null || getById(dto.getId()) == null);
        ShopHomeBanner banner = BeanCopyUtils.copy(dto, ShopHomeBanner.class);
        saveOrUpdate(banner);
    }

    @Override
    public PageResult<ShopHomeBannerVO> page(ShopHomeBannerDTO.ListQuery dto) {
        PageUtils.toPage(dto);
        try {
            SimpleDataScopeHelper.start(ShopHomeBanner.class);
            List<ShopHomeBannerVO> list = mapper.selectBannerList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ShopHomeBannerVO> list(ShopHomeBannerDTO.ListQuery dto) {
        try {
            SimpleDataScopeHelper.start(ShopHomeBanner.class);
            return mapper.selectBannerList(dto);
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
    public ShopHomeBannerVO detail(Object id) {
        ShopHomeBanner banner = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(banner);
        return BeanCopyUtils.copy(banner, ShopHomeBannerVO.class);
    }

    @Override
    public List<ShopHomeBannerVO> frontList(Integer size) {
        int limit = resolveFrontSize(size);
        List<ShopHomeBannerVO> list = mapper.selectFrontBannerList(LocalDateTime.now(), limit);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list;
    }

    private ShopHomeBanner buildCreateEntity(ShopHomeBannerDTO.Create dto) {
        ShopHomeBanner banner = BeanCopyUtils.copy(dto, ShopHomeBanner.class);
        if (banner.getStatus() == null) {
            banner.setStatus(0);
        }
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
        if (banner.getIsDel() == null) {
            banner.setIsDel(0);
        }
        return banner;
    }

    private int resolveFrontSize(Integer size) {
        return (size == null || size <= 0) ? DEFAULT_FRONT_SIZE : Math.min(size, MAX_FRONT_SIZE);
    }
}
