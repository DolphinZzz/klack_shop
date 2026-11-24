package com.sz.admin.shop_system.mapper;

import com.mybatisflex.core.BaseMapper;
import com.sz.admin.shop_system.pojo.dto.ShopExpressTemplateDTO;
import com.sz.admin.shop_system.pojo.po.ShopExpressTemplate;
import com.sz.admin.shop_system.pojo.vo.ShopExpressTemplateVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* <p>
* 快递模板表 Mapper 接口
* </p>
*
* @author car
* @since 2026-01-22
*/
public interface ShopExpressTemplateMapper extends BaseMapper<ShopExpressTemplate> {

    List<ShopExpressTemplateVO> selectExpressTemplatePageList(@Param("query") ShopExpressTemplateDTO.ListQuery query);

    List<ShopExpressTemplateVO> selectExpressTemplateList(@Param("query") ShopExpressTemplateDTO.ListQuery query);

    ShopExpressTemplateVO selectExpressTemplateDetailById(@Param("id") Object id);

    ShopExpressTemplate selectActiveById(@Param("id") Long id);
}