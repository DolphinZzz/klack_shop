package com.sz.admin.shop_system.service;

import com.sz.admin.shop_system.pojo.dto.ShopExpressTemplateDTO;

import com.mybatisflex.core.service.IService;
import com.sz.admin.shop_system.pojo.po.ShopExpressTemplate;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.entity.PageResult;
import java.util.List;
import com.sz.admin.shop_system.pojo.vo.ShopExpressTemplateVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>
 * 快递模板表 Service
 * </p>
 *
 * @author car
 * @since 2026-01-22
 */
public interface ShopExpressTemplateService extends IService<ShopExpressTemplate> {

    void create(ShopExpressTemplateDTO.Create dto);

    void update(ShopExpressTemplateDTO.Update dto);

    PageResult<ShopExpressTemplateVO> page(ShopExpressTemplateDTO.ListQuery dto);

    List<ShopExpressTemplateVO> list(ShopExpressTemplateDTO.ListQuery dto);

    void remove(SelectIdsDTO dto);

    ShopExpressTemplateVO detail(Object id);

    void exportExcel(ShopExpressTemplateDTO.ListQuery dto, HttpServletResponse response);
}