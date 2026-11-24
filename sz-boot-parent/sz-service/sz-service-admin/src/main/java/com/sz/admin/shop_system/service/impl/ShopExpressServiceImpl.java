package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopExpressMapper;
import com.sz.admin.shop_system.pojo.dto.ShopExpressDTO;
import com.sz.admin.shop_system.pojo.po.ShopExpress;
import com.sz.admin.shop_system.pojo.vo.ShopExpressVO;
import com.sz.admin.shop_system.service.ShopExpressService;
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
 * 快递表 服务实现类
 * </p>
 *
 * @author car
 * @since 2026-01-22
 */
@Service
@RequiredArgsConstructor
public class ShopExpressServiceImpl extends ServiceImpl<ShopExpressMapper, ShopExpress> implements ShopExpressService {

    @Override
    public void create(ShopExpressDTO.Create dto) {
        ShopExpress shopExpress = BeanCopyUtils.copy(dto, ShopExpress.class);
        save(shopExpress);
    }

    @Override
    public void update(ShopExpressDTO.Update dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getId() == null || getById(dto.getId()) == null);
        ShopExpress shopExpress = BeanCopyUtils.copy(dto, ShopExpress.class);
        saveOrUpdate(shopExpress);
    }

    @Override
    public PageResult<ShopExpressVO> page(ShopExpressDTO.ListQuery dto) {
        PageUtils.toPage(dto);
        try {
            SimpleDataScopeHelper.start(ShopExpress.class);
            List<ShopExpressVO> list = mapper.selectExpressList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ShopExpressVO> list(ShopExpressDTO.ListQuery dto) {
        try {
            SimpleDataScopeHelper.start(ShopExpress.class);
            return mapper.selectExpressList(dto);
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
    public ShopExpressVO detail(Object id) {
        ShopExpress shopExpress = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(shopExpress);
        return BeanCopyUtils.copy(shopExpress, ShopExpressVO.class);
    }
}
