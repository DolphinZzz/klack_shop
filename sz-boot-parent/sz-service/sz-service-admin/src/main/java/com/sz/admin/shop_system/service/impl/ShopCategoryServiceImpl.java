package com.sz.admin.shop_system.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.mapper.ShopCategoryMapper;
import com.sz.admin.shop_system.pojo.dto.ShopCategoryDTO;
import com.sz.admin.shop_system.pojo.po.ShopCategory;
import com.sz.admin.shop_system.pojo.vo.ShopCategoryVO;
import com.sz.admin.shop_system.service.ShopCategoryService;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.datascope.SimpleDataScopeHelper;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.PageUtils;
import com.sz.core.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 分类管理 服务实现类
 * </p>
 *
 * @author car
 * @since 2025-11-26
 */
@Service
@RequiredArgsConstructor
public class ShopCategoryServiceImpl extends ServiceImpl<ShopCategoryMapper, ShopCategory> implements ShopCategoryService {

    @Override
    public void create(ShopCategoryDTO.Create dto) {
        ShopCategory shopCategory = BeanCopyUtils.copy(dto, ShopCategory.class);
        save(shopCategory);
    }

    @Override
    public void update(ShopCategoryDTO.Update dto) {
        validateCategoryExists(dto.getId());
        ShopCategory shopCategory = BeanCopyUtils.copy(dto, ShopCategory.class);
        saveOrUpdate(shopCategory);
    }

    @Override
    public PageResult<ShopCategoryVO> page(ShopCategoryDTO.ListQuery dto) {
        try {
            SimpleDataScopeHelper.start(ShopCategory.class);
            List<ShopCategory> shopCategories = mapper.selectAllCategories();
            List<ShopCategory> filteredCategories = filterRootCategoriesByCateName(shopCategories, dto.getCateName());
            List<ShopCategoryVO> tree = buildTree(filteredCategories);
            List<ShopCategoryVO> pageData = sliceTreePage(tree, dto.getPage(), dto.getLimit());
            return PageUtils.getPageResult(dto.getPage(), dto.getLimit(), pageData, tree.size());
        } finally {
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    public List<ShopCategoryVO> buildTree(List<ShopCategory> allCategory) {
        Map<Integer, ShopCategoryVO> categoryMap = new HashMap<>();
        List<ShopCategoryVO> trees = new ArrayList<>();

        for (ShopCategory category : allCategory) {
            ShopCategoryVO categoryVO = assembleCategoryNode(category);
            categoryMap.put(categoryVO.getId(), categoryVO);
        }

        for (ShopCategoryVO categoryVO : categoryMap.values()) {
            if (categoryVO.getPid() != null && categoryVO.getPid() == 0) {
                trees.add(categoryVO);
            } else {
                ShopCategoryVO parent = categoryMap.get(categoryVO.getPid());
                if (parent != null) {
                    parent.getChildren().add(categoryVO);
                }
            }
        }

        return trees;
    }

    @Override
    public List<ShopCategoryVO> list(ShopCategoryDTO.ListQuery dto) {
        try {
            SimpleDataScopeHelper.start(ShopCategory.class);
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
    public ShopCategoryVO detail(Object id) {
        try {
            SimpleDataScopeHelper.start(ShopCategory.class);
            ShopCategory shopCategory = getById((Serializable) id);
            CommonResponseEnum.INVALID_ID.assertNull(shopCategory);

            List<ShopCategory> childCategories = mapper.selectChildrenByPid(shopCategory.getId());
            return assembleCategoryDetail(shopCategory, childCategories);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    @Override
    public List<ShopCategoryVO> cateChildrenList() {
        try {
            SimpleDataScopeHelper.start(ShopCategory.class);
            List<ShopCategory> shopCategories = mapper.selectAllCategories();
            return buildTree(shopCategories);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    @Override
    public List<ShopCategory> getByPIds(List<Integer> idList) {
        if (idList == null || idList.isEmpty()) {
            return Collections.emptyList();
        }
        return mapper.selectByPids(idList);
    }

    private void validateCategoryExists(Long id) {
        CommonResponseEnum.INVALID_ID.assertTrue(id == null || getById(id) == null);
    }

    private ShopCategoryVO assembleCategoryNode(ShopCategory category) {
        ShopCategoryVO categoryVO = new ShopCategoryVO();
        categoryVO.setId(Math.toIntExact(category.getId()));
        categoryVO.setPid(category.getPid());
        categoryVO.setCateName(category.getCateName());
        categoryVO.setSort(category.getSort());
        categoryVO.setChildren(new ArrayList<>());
        return categoryVO;
    }

    private ShopCategoryVO assembleCategoryDetail(ShopCategory shopCategory, List<ShopCategory> childCategories) {
        ShopCategoryVO categoryVO = BeanCopyUtils.copy(shopCategory, ShopCategoryVO.class);
        if (childCategories != null && !childCategories.isEmpty()) {
            List<ShopCategoryVO> children = childCategories.stream()
                    .map(child -> BeanCopyUtils.copy(child, ShopCategoryVO.class))
                    .collect(Collectors.toList());
            categoryVO.setChildren(children);
        } else {
            categoryVO.setChildren(new ArrayList<>());
        }
        return categoryVO;
    }

    private List<ShopCategory> filterRootCategoriesByCateName(List<ShopCategory> allCategories, String cateName) {
        if (StringUtils.isNull(cateName)) {
            return allCategories;
        }
        return allCategories.stream()
                .filter(category -> category.getPid() != null
                        && category.getPid() == 0
                        && category.getCateName() != null
                        && category.getCateName().contains(cateName))
                .collect(Collectors.toList());
    }

    private List<ShopCategoryVO> sliceTreePage(List<ShopCategoryVO> tree, int page, int limit) {
        int start = Math.max((page - 1) * limit, 0);
        if (start >= tree.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(start + limit, tree.size());
        return tree.subList(start, end);
    }
}
