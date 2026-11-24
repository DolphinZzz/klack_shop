package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopExpressTemplateMapper;
import com.sz.admin.shop_system.pojo.dto.ShopExpressTemplateDTO;
import com.sz.admin.shop_system.pojo.po.ShopExpressTemplate;
import com.sz.admin.shop_system.pojo.vo.ShopExpressTemplateVO;
import com.sz.admin.shop_system.service.ShopExpressTemplateService;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.datascope.SimpleDataScopeHelper;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.FileUtils;
import com.sz.core.util.PageUtils;
import com.sz.excel.utils.ExcelUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;

/**
 * <p>
 * 快递模板表 服务实现类
 * </p>
 *
 * @author car
 * @since 2026-01-22
 */
@Service
@RequiredArgsConstructor
public class ShopExpressTemplateServiceImpl extends ServiceImpl<ShopExpressTemplateMapper, ShopExpressTemplate> implements ShopExpressTemplateService {

    @Override
    public void create(ShopExpressTemplateDTO.Create dto) {
        ShopExpressTemplate shopExpressTemplate = BeanCopyUtils.copy(dto, ShopExpressTemplate.class);
        save(shopExpressTemplate);
    }

    @Override
    public void update(ShopExpressTemplateDTO.Update dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getId() == null || getById(dto.getId()) == null);
        ShopExpressTemplate shopExpressTemplate = BeanCopyUtils.copy(dto, ShopExpressTemplate.class);
        saveOrUpdate(shopExpressTemplate);
    }

    @Override
    public PageResult<ShopExpressTemplateVO> page(ShopExpressTemplateDTO.ListQuery dto) {
        PageUtils.toPage(dto);
        try {
            SimpleDataScopeHelper.start(ShopExpressTemplate.class);
            List<ShopExpressTemplateVO> list = mapper.selectExpressTemplatePageList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ShopExpressTemplateVO> list(ShopExpressTemplateDTO.ListQuery dto) {
        try {
            SimpleDataScopeHelper.start(ShopExpressTemplate.class);
            return mapper.selectExpressTemplateList(dto);
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
    public ShopExpressTemplateVO detail(Object id) {
        ShopExpressTemplateVO vo = mapper.selectExpressTemplateDetailById(id);
        CommonResponseEnum.INVALID_ID.assertNull(vo);
        return vo;
    }

    @SneakyThrows
    @Override
    public void exportExcel(ShopExpressTemplateDTO.ListQuery dto, HttpServletResponse response) {
        List<ShopExpressTemplateVO> list = list(dto);
        String fileName = "快递模板模板";
        OutputStream os = FileUtils.getOutputStream(response, fileName + ".xlsx");
        ExcelUtils.exportExcel(list, "快递模板", ShopExpressTemplateVO.class, os);
    }
}
