package com.sz.admin.shop_system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopHomeNewsMapper;
import com.sz.admin.shop_system.pojo.dto.ShopHomeNewsDTO;
import com.sz.admin.shop_system.pojo.po.ShopHomeNews;
import com.sz.admin.shop_system.pojo.po.ShopHomeNewsCategory;
import com.sz.admin.shop_system.pojo.vo.ShopHomeNewsVO;
import com.sz.admin.shop_system.service.ShopHomeNewsCategoryService;
import com.sz.admin.shop_system.service.ShopHomeNewsService;
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
 * 首页资讯 Service 实现
 *
 * @author car
 * @since 2026-03-28
 */
@Service
@RequiredArgsConstructor
public class ShopHomeNewsServiceImpl extends ServiceImpl<ShopHomeNewsMapper, ShopHomeNews> implements ShopHomeNewsService {

    private static final int MAX_FRONT_SIZE = 10;
    private static final int DEFAULT_FRONT_LIST_SIZE = 3;
    private static final int DEFAULT_HOT_LIST_SIZE = 5;

    private final ShopHomeNewsCategoryService lbHomeNewsCategoryService;

    @Override
    public void create(ShopHomeNewsDTO.Create dto) {
        ShopHomeNews news = buildCreateEntity(dto);
        save(news);
    }

    @Override
    public void update(ShopHomeNewsDTO.Update dto) {
        ShopHomeNews dbNews = dto.getId() == null ? null : getById(dto.getId());
        CommonResponseEnum.INVALID_ID.assertNull(dbNews);
        ShopHomeNews news = buildUpdateEntity(dto, dbNews);
        saveOrUpdate(news);
    }

    @Override
    public PageResult<ShopHomeNewsVO> page(ShopHomeNewsDTO.ListQuery dto) {
        PageUtils.toPage(dto);
        try {
            SimpleDataScopeHelper.start(ShopHomeNews.class);
            List<ShopHomeNewsVO> list = mapper.selectNewsList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ShopHomeNewsVO> list(ShopHomeNewsDTO.ListQuery dto) {
        try {
            SimpleDataScopeHelper.start(ShopHomeNews.class);
            return mapper.selectNewsList(dto);
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
    public ShopHomeNewsVO detail(Object id) {
        ShopHomeNews news = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(news);
        return BeanCopyUtils.copy(news, ShopHomeNewsVO.class);
    }

    @Override
    public List<ShopHomeNewsVO> frontList(Integer size) {
        int limit = resolveFrontSize(size, DEFAULT_FRONT_LIST_SIZE);
        List<ShopHomeNewsVO> list = mapper.selectFrontNewsList(LocalDateTime.now(), limit);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list;
    }

    @Override
    public PageResult<ShopHomeNewsVO> frontPage(ShopHomeNewsDTO.FrontPage dto) {
        PageUtils.toPage(dto);
        try {
            List<ShopHomeNewsVO> list = mapper.selectFrontNewsPageList(LocalDateTime.now(), dto.getCategoryId());
            return PageUtils.getPageResult(list);
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ShopHomeNewsVO> hotList(Integer size) {
        int limit = resolveFrontSize(size, DEFAULT_HOT_LIST_SIZE);
        List<ShopHomeNewsVO> list = mapper.selectHotNewsList(LocalDateTime.now(), limit);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list;
    }

    @Override
    public ShopHomeNewsVO frontDetail(Object id) {
        ShopHomeNews news = mapper.selectFrontNewsById(id, LocalDateTime.now());
        CommonResponseEnum.INVALID_ID.assertNull(news);
        increaseBrowseCount(news);
        return BeanCopyUtils.copy(news, ShopHomeNewsVO.class);
    }

    private ShopHomeNews buildCreateEntity(ShopHomeNewsDTO.Create dto) {
        ShopHomeNews news = BeanCopyUtils.copy(dto, ShopHomeNews.class);
        news.setTagName(resolveTagName(news.getCategoryId(), news.getTagName()));
        if (news.getStatus() == null) {
            news.setStatus(0);
        }
        if (news.getIsTop() == null) {
            news.setIsTop(0);
        }
        if (news.getSort() == null) {
            news.setSort(0);
        }
        if (news.getPublishTime() == null) {
            news.setPublishTime(LocalDateTime.now());
        }
        if (news.getBrowseCount() == null) {
            news.setBrowseCount(0);
        }
        if (news.getIsDel() == null) {
            news.setIsDel(0);
        }
        return news;
    }

    private ShopHomeNews buildUpdateEntity(ShopHomeNewsDTO.Update dto, ShopHomeNews dbNews) {
        ShopHomeNews news = BeanCopyUtils.copy(dto, ShopHomeNews.class);
        news.setTagName(resolveTagName(news.getCategoryId(), news.getTagName()));
        if (news.getPublishTime() == null) {
            news.setPublishTime(LocalDateTime.now());
        }
        if (news.getBrowseCount() == null) {
            news.setBrowseCount(dbNews.getBrowseCount() == null ? 0 : dbNews.getBrowseCount());
        }
        return news;
    }

    private void increaseBrowseCount(ShopHomeNews news) {
        news.setBrowseCount((news.getBrowseCount() == null ? 0 : news.getBrowseCount()) + 1);
        updateById(news);
    }

    private int resolveFrontSize(Integer size, int defaultSize) {
        return (size == null || size <= 0) ? defaultSize : Math.min(size, MAX_FRONT_SIZE);
    }

    private String resolveTagName(Long categoryId, String fallback) {
        if (categoryId == null || categoryId <= 0) {
            return StrUtil.blankToDefault(fallback, "资讯");
        }
        ShopHomeNewsCategory category = lbHomeNewsCategoryService.getById(categoryId);
        if (category == null || StrUtil.isBlank(category.getCategoryName())) {
            return StrUtil.blankToDefault(fallback, "资讯");
        }
        return category.getCategoryName();
    }
}
