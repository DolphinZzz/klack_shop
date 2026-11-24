package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopOrderStatusMapper;
import com.sz.admin.shop_system.pojo.dto.ShopOrderStatusDTO;
import com.sz.admin.shop_system.pojo.po.ShopOrderStatus;
import com.sz.admin.shop_system.pojo.vo.ShopOrderStatusVO;
import com.sz.admin.shop_system.service.ShopOrderStatusService;
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
 * 订单状态表 服务实现类
 * </p>
 *
 * @author car
 * @since 2026-01-29
 */
@Service
@RequiredArgsConstructor
public class ShopOrderStatusServiceImpl extends ServiceImpl<ShopOrderStatusMapper, ShopOrderStatus> implements ShopOrderStatusService {

    @Override
    public void create(ShopOrderStatusDTO.Create dto) {
        ShopOrderStatus shopOrderStatus = BeanCopyUtils.copy(dto, ShopOrderStatus.class);
        save(shopOrderStatus);
    }

    @Override
    public void update(ShopOrderStatusDTO.Update dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getId() == null || getById(dto.getId()) == null);
        ShopOrderStatus shopOrderStatus = BeanCopyUtils.copy(dto, ShopOrderStatus.class);
        saveOrUpdate(shopOrderStatus);
    }

    @Override
    public PageResult<ShopOrderStatusVO> page(ShopOrderStatusDTO.ListQuery dto) {
        PageUtils.toPage(dto);
        try {
            SimpleDataScopeHelper.start(ShopOrderStatus.class);
            List<ShopOrderStatusVO> list = mapper.selectOrderStatusList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ShopOrderStatusVO> list(ShopOrderStatusDTO.ListQuery dto) {
        try {
            SimpleDataScopeHelper.start(ShopOrderStatus.class);
            return mapper.selectOrderStatusList(dto);
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
    public ShopOrderStatusVO detail(Object id) {
        ShopOrderStatus shopOrderStatus = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(shopOrderStatus);
        return BeanCopyUtils.copy(shopOrderStatus, ShopOrderStatusVO.class);
    }
}
