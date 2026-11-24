package com.sz.admin.shop_system.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopSystemCityMapper;
import com.sz.admin.shop_system.pojo.po.ShopSystemCity;
import com.sz.admin.shop_system.pojo.vo.ShopSystemCityVO;
import com.sz.admin.shop_system.service.ShopSystemCityService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ShopSystemCityServiceImpl extends ServiceImpl<ShopSystemCityMapper, ShopSystemCity> implements ShopSystemCityService {

    @Override
    public List<ShopSystemCityVO> listByParentAndRegionType(Integer parentId, Integer regionType) {
        Integer level = mapRegionTypeToLevel(regionType);
        Integer targetParentId = resolveParentId(parentId, regionType);
        if (level == null || targetParentId == null) {
            return new ArrayList<>();
        }

        List<ShopSystemCity> rows = mapper.selectByLevelAndParentId(level, targetParentId);
        return assembleCityVOList(rows, regionType);
    }

    private Integer resolveParentId(Integer parentId, Integer regionType) {
        if (regionType == null) {
            return null;
        }
        // 省份查询固定查 parent_id=0；市/区/街道使用上级 city_id 过滤。
        return regionType == 1 ? 0 : parentId;
    }

    private List<ShopSystemCityVO> assembleCityVOList(List<ShopSystemCity> rows, Integer regionType) {
        List<ShopSystemCityVO> result = new ArrayList<>(rows.size());
        for (ShopSystemCity row : rows) {
            ShopSystemCityVO vo = new ShopSystemCityVO();
            vo.setId(row.getId());
            vo.setCityId(row.getCityId());
            vo.setParentId(row.getParentId());
            vo.setRegionType(regionType);
            vo.setName(row.getName());
            result.add(vo);
        }
        return result;
    }

    private Integer mapRegionTypeToLevel(Integer regionType) {
        if (regionType == null) {
            return null;
        }
        return switch (regionType) {
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 3;
            default -> null;
        };
    }
}
