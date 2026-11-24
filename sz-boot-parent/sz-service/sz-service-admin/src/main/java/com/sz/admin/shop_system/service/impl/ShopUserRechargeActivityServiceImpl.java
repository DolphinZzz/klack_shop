package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopUserRechargeActivityMapper;
import com.sz.admin.shop_system.pojo.dto.ShopUserRechargeActivityDTO;
import com.sz.admin.shop_system.pojo.po.ShopUserRechargeActivity;
import com.sz.admin.shop_system.pojo.vo.ShopUserRechargeActivityVO;
import com.sz.admin.shop_system.service.ShopUserRechargeActivityService;
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
 * 充值活动表 服务实现类
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
@Service
@RequiredArgsConstructor
public class ShopUserRechargeActivityServiceImpl extends ServiceImpl<ShopUserRechargeActivityMapper, ShopUserRechargeActivity> implements ShopUserRechargeActivityService {

    @Override
    public void create(ShopUserRechargeActivityDTO.Create dto) {
        ShopUserRechargeActivity shopUserRechargeActivity = BeanCopyUtils.copy(dto, ShopUserRechargeActivity.class);
        save(shopUserRechargeActivity);
    }

    @Override
    public void update(ShopUserRechargeActivityDTO.Update dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getId() == null || mapper.countById(dto.getId()) <= 0);
        ShopUserRechargeActivity shopUserRechargeActivity = BeanCopyUtils.copy(dto, ShopUserRechargeActivity.class);
        saveOrUpdate(shopUserRechargeActivity);
    }

    @Override
    public PageResult<ShopUserRechargeActivityVO> page(ShopUserRechargeActivityDTO.ListQuery dto) {
        PageUtils.toPage(dto);
        try {
            SimpleDataScopeHelper.start(ShopUserRechargeActivity.class);
            List<ShopUserRechargeActivityVO> list = mapper.selectActivityList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ShopUserRechargeActivityVO> list(ShopUserRechargeActivityDTO.ListQuery dto) {
        try {
            SimpleDataScopeHelper.start(ShopUserRechargeActivity.class);
            return mapper.selectActivityList(dto);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    @Override
    public List<ShopUserRechargeActivityVO> frontList() {
        return mapper.selectFrontActivityList();
    }

    @Override
    public void remove(SelectIdsDTO dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getIds().isEmpty());
        removeByIds(dto.getIds());
    }

    @Override
    public ShopUserRechargeActivityVO detail(Object id) {
        ShopUserRechargeActivity shopUserRechargeActivity = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(shopUserRechargeActivity);
        return BeanCopyUtils.copy(shopUserRechargeActivity, ShopUserRechargeActivityVO.class);
    }
}
