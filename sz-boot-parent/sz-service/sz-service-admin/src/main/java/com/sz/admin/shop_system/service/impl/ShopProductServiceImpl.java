package com.sz.admin.shop_system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.constants.Constant;
import com.sz.admin.shop_system.mapper.ShopProductAttrMapper;
import com.sz.admin.shop_system.mapper.ShopProductAttrValueMapper;
import com.sz.admin.shop_system.mapper.ShopProductMapper;
import com.sz.admin.shop_system.pojo.dto.ShopProductAttrDTO;
import com.sz.admin.shop_system.pojo.dto.ShopProductDTO;
import com.sz.admin.shop_system.pojo.page.ProductAttrResponse;
import com.sz.admin.shop_system.pojo.page.ProductDetailResponse;
import com.sz.admin.shop_system.pojo.page.ProductResponse;
import com.sz.admin.shop_system.pojo.po.ShopCategory;
import com.sz.admin.shop_system.pojo.po.ShopProduct;
import com.sz.admin.shop_system.pojo.po.ShopProductAttr;
import com.sz.admin.shop_system.pojo.po.ShopProductAttrValue;
import com.sz.admin.shop_system.pojo.vo.ShopFrontProductCommentVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductAttrOption;
import com.sz.admin.shop_system.pojo.vo.ShopProductAttrVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductAttrValueVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductDetailVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductVO;
import com.sz.admin.shop_system.service.ShopCategoryService;
import com.sz.admin.shop_system.service.ShopFlashSaleService;
import com.sz.admin.shop_system.service.ShopProductAttrService;
import com.sz.admin.shop_system.service.ShopProductAttrValueService;
import com.sz.admin.shop_system.service.ShopProductService;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author car
 * @since 2025-12-03
 */
@Service
@RequiredArgsConstructor
public class ShopProductServiceImpl extends ServiceImpl<ShopProductMapper, ShopProduct> implements ShopProductService {

    private final ShopCategoryService categoryService;
    private final ShopFlashSaleService shopFlashSaleService;
    private final ShopProductAttrService productAttrService;
    private final ShopProductAttrValueService productAttrValueService;
    private final ShopProductAttrMapper productAttrMapper;
    private final ShopProductAttrValueMapper productAttrValueMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(ShopProductDTO.Create dto) {
        validateWritePayload(dto.getAttrList(), dto.getAttrValueList(), dto.getSliderImages());
        normalizeDefaultFlag(dto.getAttrValueList());

        ShopProduct shopProduct = buildProductForCreate(dto);
        save(shopProduct);

        Long productId = shopProduct.getId();
        persistAttrsAndSkus(productId, dto.getAttrList(), dto.getAttrValueList());
        updateProductSummaryFromSkus(productId, dto.getAttrValueList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ShopProductDTO.Update dto) {
        Long productId = dto.getId();
        CommonResponseEnum.INVALID_ID.assertNull(productId);
        validateWritePayload(dto.getAttrList(), dto.getAttrValueList(), dto.getSliderImages());
        normalizeDefaultFlag(dto.getAttrValueList());

        ShopProduct updateBase = buildProductForUpdate(dto, productId);
        updateById(updateBase);

        replaceAttrsAndSkus(productId, dto.getAttrList(), dto.getAttrValueList());
        updateProductSummaryFromSkus(productId, dto.getAttrValueList());
    }

    @Override
    public PageResult<ShopProductVO> page(ShopProductDTO.ListQuery dto) {
        SimpleDataScopeHelper.start(ShopProduct.class);
        PageUtils.toPage(dto);
        try {
            List<ShopProductVO> list = mapper.selectProductList(dto);
            return PageUtils.getPageResult(list);
        } finally {
            PageHelper.clearPage();
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    @Override
    public List<ShopProductVO> list(ShopProductDTO.ListQuery dto) {
        SimpleDataScopeHelper.start(ShopProduct.class);
        try {
            return mapper.selectProductList(dto);
        } finally {
            SimpleDataScopeHelper.clearDataScope();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(SelectIdsDTO dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getIds().isEmpty());
        productAttrValueMapper.deleteByProductIds(dto.getIds());
        productAttrMapper.deleteByProductIds(dto.getIds());
        removeByIds(dto.getIds());
    }

    @Override
    public ShopProductDetailVO detail(Object id) {
        ShopProduct shopProduct = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(shopProduct);

        List<ShopProductAttr> productAttrs = productAttrMapper.selectByProductId(shopProduct.getId());
        List<ShopProductAttrValue> attrValues = productAttrValueMapper.selectByProductId(shopProduct.getId());
        return assembleProductDetailVO(shopProduct, productAttrs, attrValues);
    }

    @SneakyThrows
    @Override
    public void exportExcel(ShopProductDTO.ListQuery dto, HttpServletResponse response) {
        List<ShopProductVO> list = list(dto);
        String fileName = "商品表模板";
        OutputStream os = FileUtils.getOutputStream(response, fileName + ".xlsx");
        ExcelUtils.exportExcel(list, "商品表", ShopProductVO.class, os);
    }

    @Override
    public PageResult<ProductResponse> getCategoryProductList(ShopProductDTO.FrontList request) {
        List<Integer> categoryIdList = resolveCategoryIds(request.getCid());
        PageUtils.toPage(request);
        try {
            List<ProductResponse> list = mapper.selectFrontCategoryProductList(
                    categoryIdList,
                    request.getKeyword(),
                    request.getMinPrice(),
                    request.getMaxPrice(),
                    request.getSalesOrder(),
                    request.getPriceOrder()
            );
            return PageUtils.getPageResult(list);
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public ProductDetailResponse getDetail(Integer id, String type) {
        ShopProduct product = this.getById(id);
        CommonResponseEnum.INVALID_ID.assertNull(product);

        List<ShopProductAttr> productAttrList = loadProductAttrsWithFallback(product.getId());
        List<ShopProductAttrValue> productAttrValueList = productAttrValueService.getListByProductId(product.getId());

        return assembleFrontProductDetail(product, productAttrList, productAttrValueList);
    }

    @Override
    public PageResult<ShopFrontProductCommentVO> getFrontCommentPage(Integer productId, ShopProductDTO.FrontCommentPage dto) {
        ShopProductDTO.FrontCommentPage query = dto == null ? new ShopProductDTO.FrontCommentPage() : dto;
        PageUtils.toPage(query);
        try {
            List<ShopFrontProductCommentVO> list = mapper.selectFrontCommentList(productId);
            return PageUtils.getPageResult(list);
        } finally {
            PageHelper.clearPage();
        }
    }

    private void validateWritePayload(List<ShopProductAttrDTO.Create> attrList,
                                      List<ShopProductAttrDTO.Value> attrValueList,
                                      List<String> sliderImages) {
        CommonResponseEnum.UNKNOWN.assertNull(attrList);
        CommonResponseEnum.UNKNOWN.assertNull(attrValueList);
        CommonResponseEnum.UNKNOWN.assertNull(sliderImages);
    }

    private ShopProduct buildProductForCreate(ShopProductDTO.Create dto) {
        ShopProduct shopProduct = BeanCopyUtils.copy(dto, ShopProduct.class);
        shopProduct.setSliderImage(String.join(",", dto.getSliderImages()));
        normalizeShippingFields(shopProduct);
        return shopProduct;
    }

    private ShopProduct buildProductForUpdate(ShopProductDTO.Update dto, Long productId) {
        ShopProduct updateBase = BeanCopyUtils.copy(dto, ShopProduct.class);
        updateBase.setId(productId);
        updateBase.setSliderImage(String.join(",", dto.getSliderImages()));
        normalizeShippingFields(updateBase);
        return updateBase;
    }

    private void persistAttrsAndSkus(Long productId,
                                     List<ShopProductAttrDTO.Create> attrList,
                                     List<ShopProductAttrDTO.Value> attrValueList) {
        List<ShopProductAttr> attrs = buildProductAttrs(productId, attrList);
        productAttrService.saveBatch(attrs);

        List<ShopProductAttrValue> values = buildProductAttrValues(productId, attrValueList);
        productAttrValueService.saveBatch(values);
    }

    private void replaceAttrsAndSkus(Long productId,
                                     List<ShopProductAttrDTO.Create> attrList,
                                     List<ShopProductAttrDTO.Value> attrValueList) {
        productAttrMapper.deleteByProductId(productId);
        productAttrValueMapper.deleteByProductId(productId);
        persistAttrsAndSkus(productId, attrList, attrValueList);
    }

    private void updateProductSummaryFromSkus(Long productId, List<ShopProductAttrDTO.Value> attrValueList) {
        ShopProduct summaryUpdate = buildProductSummaryUpdate(productId, attrValueList);
        updateById(summaryUpdate);
    }

    private List<ShopProductAttr> buildProductAttrs(Long productId, List<ShopProductAttrDTO.Create> attrList) {
        List<ShopProductAttr> attrs = new ArrayList<>(attrList.size());
        for (ShopProductAttrDTO.Create a : attrList) {
            List<ShopProductAttrDTO.Option> optionList = a.getOptionList();
            CommonResponseEnum.UNKNOWN.assertNull(optionList);

            String attrValues = optionList.stream()
                    .map(ShopProductAttrDTO.Option::getOptionName)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .collect(Collectors.joining(","));

            attrs.add(new ShopProductAttr()
                    .setAttrName(a.getAttrName())
                    .setProductId(productId)
                    .setAttrValues(attrValues)
                    .setType(Constant.PRODUCT_TYPE_NORMAL)
                    .setIsDel(0));
        }
        return attrs;
    }

    private List<ShopProductAttrValue> buildProductAttrValues(Long productId, List<ShopProductAttrDTO.Value> attrValueList) {
        List<ShopProductAttrValue> values = new ArrayList<>(attrValueList.size());
        for (ShopProductAttrDTO.Value v : attrValueList) {
            ShopProductAttrValue value = BeanCopyUtils.copy(v, ShopProductAttrValue.class);
            value.setProductId(productId);
            value.setSku(String.join(",", v.getAttrArr()));
            value.setIsDefault(Boolean.TRUE.equals(v.getIsDefault()) ? 1 : 0);
            value.setIsDel(0);
            value.setVersion(0);
            values.add(value);
        }
        return values;
    }

    private ShopProduct buildProductSummaryUpdate(Long productId, List<ShopProductAttrDTO.Value> attrValueList) {
        int totalStock = attrValueList.stream()
                .map(ShopProductAttrDTO.Value::getStock)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        ShopProductAttrDTO.Value defaultSku = attrValueList.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                .findFirst()
                .orElse(attrValueList.get(0));

        ShopProduct update = new ShopProduct();
        update.setId(productId);
        update.setStock(totalStock);
        update.setPrice(defaultSku.getPrice());
        update.setOtPrice(defaultSku.getOtPrice());
        update.setGiveIntegral(resolveGiveIntegralByPrice(defaultSku.getPrice()));
        if (StrUtil.isNotBlank(defaultSku.getImage())) {
            update.setImage(defaultSku.getImage());
        }
        return update;
    }

    private ShopProductDetailVO assembleProductDetailVO(ShopProduct shopProduct,
                                                      List<ShopProductAttr> productAttrs,
                                                      List<ShopProductAttrValue> attrValueList) {
        ShopProductDetailVO productDetailVO = BeanCopyUtils.copy(shopProduct, ShopProductDetailVO.class);

        List<ShopProductAttrVO> productAttrVOList = buildAttrVOList(productAttrs, shopProduct);
        productDetailVO.setAttrList(productAttrVOList);

        List<String> attrNameList = productAttrVOList.stream()
                .map(ShopProductAttrVO::getAttrName)
                .toList();
        productDetailVO.setAttrValueList(buildAttrValueVOList(attrValueList, attrNameList));
        return productDetailVO;
    }

    private ProductDetailResponse assembleFrontProductDetail(ShopProduct product,
                                                             List<ShopProductAttr> productAttrList,
                                                             List<ShopProductAttrValue> productAttrValueList) {
        ProductDetailResponse productDetailResponse = new ProductDetailResponse();
        productDetailResponse.setProductInfo(product);
        productDetailResponse.setProductAttr(buildProductAttrResponses(productAttrList));

        HashMap<String, Object> skuMap = new HashMap<>();
        for (ShopProductAttrValue productAttrValue : productAttrValueList) {
            ShopProductAttrValueVO productAttrValueVo = BeanCopyUtils.copy(productAttrValue, ShopProductAttrValueVO.class);
            skuMap.put(productAttrValue.getSku(), productAttrValueVo);
        }
        productDetailResponse.setProductValue(skuMap);
        productDetailResponse.setFlashSale(shopFlashSaleService.frontDetail(product.getId()));
        return productDetailResponse;
    }

    private List<ShopProductAttr> loadProductAttrsWithFallback(Long productId) {
        List<ShopProductAttr> productAttrList = productAttrService.getListByProductIdAndType(productId, Constant.PRODUCT_TYPE_NORMAL);
        if (productAttrList == null || productAttrList.isEmpty()) {
            return productAttrMapper.selectByProductId(productId);
        }
        return productAttrList;
    }

    private List<Integer> resolveCategoryIds(String cid) {
        if (ObjectUtil.isNull(cid) || cid.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> cidList = Stream.of(cid.split(","))
                .map(Integer::valueOf)
                .collect(Collectors.toList());
        List<ShopCategory> categoryListByPid = categoryService.getByPIds(cidList);
        List<Integer> categoryIdList = categoryListByPid.stream()
                .map(ShopCategory::getId)
                .map(Long::intValue)
                .collect(Collectors.toList());
        categoryIdList.addAll(cidList);
        return categoryIdList;
    }

    private List<ShopProductAttrValueVO> buildAttrValueVOList(List<ShopProductAttrValue> attrValueList,
                                                            List<String> attrNameList) {
        List<ShopProductAttrValueVO> result = new ArrayList<>(attrValueList.size());
        for (ShopProductAttrValue attrValue : attrValueList) {
            ShopProductAttrValueVO attrValueVO = BeanCopyUtils.copy(attrValue, ShopProductAttrValueVO.class);

            String sku = attrValue.getSku();
            String[] skuValues = sku == null ? new String[0] : sku.split(",");
            Map<String, String> attrValueMap = new LinkedHashMap<>();
            for (int i = 0; i < attrNameList.size(); i++) {
                if (i < skuValues.length) {
                    attrValueMap.put(attrNameList.get(i), skuValues[i].trim());
                }
            }
            attrValueVO.setAttrValue(JSONUtil.toJsonStr(attrValueMap));
            result.add(attrValueVO);
        }
        return result;
    }

    private static List<ShopProductAttrVO> buildAttrVOList(List<ShopProductAttr> productAttrs, ShopProduct shopProduct) {
        List<ShopProductAttrVO> productAttrVOList = new ArrayList<>();
        for (ShopProductAttr attr : productAttrs) {
            ShopProductAttrVO attrVO = new ShopProductAttrVO();
            attrVO.setId(attr.getId());
            attrVO.setProductId(attr.getProductId());
            attrVO.setAttrName(attr.getAttrName());
            attrVO.setIsDel(attr.getIsDel());

            String attrValues = attr.getAttrValues();
            String[] split = attrValues == null ? new String[0] : attrValues.split(",");

            List<ShopProductAttrOption> optionList = new ArrayList<>();
            for (String opt : split) {
                ShopProductAttrOption attrOption = new ShopProductAttrOption();
                attrOption.setAttrId(attr.getId());
                attrOption.setOptionName(opt.trim());
                attrOption.setProductId(shopProduct.getId());
                optionList.add(attrOption);
            }

            attrVO.setOptionList(optionList);
            productAttrVOList.add(attrVO);
        }
        return productAttrVOList;
    }

    private List<ProductAttrResponse> buildProductAttrResponses(List<ShopProductAttr> productAttrList) {
        List<ProductAttrResponse> productAttr = new ArrayList<>();
        for (ShopProductAttr attr : productAttrList) {
            ProductAttrResponse productAttrResponse = new ProductAttrResponse();
            productAttrResponse.setId(attr.getId());
            productAttrResponse.setAttrName(attr.getAttrName());

            String[] values = attr.getAttrValues() == null ? new String[0] : attr.getAttrValues().split(",");
            List<ShopProductAttrVO> attrVOList = new ArrayList<>();
            for (String value : values) {
                ShopProductAttrVO attrVO = new ShopProductAttrVO();
                attrVO.setProductId(attr.getProductId());
                attrVO.setAttrName(value);
                attrVO.setIsDel(attr.getIsDel());
                attrVOList.add(attrVO);
            }
            productAttrResponse.setOptionList(attrVOList);
            productAttr.add(productAttrResponse);
        }
        return productAttr;
    }

    private void normalizeDefaultFlag(List<ShopProductAttrDTO.Value> list) {
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("attrValueList 不能为空");
        }

        long trueCount = list.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                .count();

        if (trueCount == 0) {
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setIsDefault(i == 0);
            }
            return;
        }

        if (trueCount > 1) {
            throw new RuntimeException("attrValueList 中 isDefault=true 只能有一个");
        }

        for (ShopProductAttrDTO.Value v : list) {
            v.setIsDefault(Boolean.TRUE.equals(v.getIsDefault()));
        }
    }

    private void normalizeShippingFields(ShopProduct product) {
        if (product == null) {
            return;
        }
        if (product.getExpressId() == null) {
            product.setIsPostage(1);
            product.setExpressName("包邮");
            product.setPostage(BigDecimal.ZERO);
            return;
        }

        product.setIsPostage(0);
        if (product.getPostage() == null || product.getPostage().compareTo(BigDecimal.ZERO) < 0) {
            product.setPostage(BigDecimal.ZERO);
        }
    }

    private static BigDecimal resolveGiveIntegralByPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.DOWN);
        }
        return price.multiply(BigDecimal.valueOf(100L)).setScale(2, RoundingMode.DOWN);
    }
}
