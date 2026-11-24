package com.sz.admin.shop_system.service.impl;

import cn.hutool.json.JSONUtil;
import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.constants.Constant;
import com.sz.admin.shop_system.mapper.ShopFlashSaleMapper;
import com.sz.admin.shop_system.mapper.ShopProductAttrMapper;
import com.sz.admin.shop_system.mapper.ShopProductAttrValueMapper;
import com.sz.admin.shop_system.mapper.ShopProductMapper;
import com.sz.admin.shop_system.pojo.dto.ShopFlashSaleDTO;
import com.sz.admin.shop_system.pojo.po.ShopFlashSale;
import com.sz.admin.shop_system.pojo.po.ShopProduct;
import com.sz.admin.shop_system.pojo.po.ShopProductAttr;
import com.sz.admin.shop_system.pojo.po.ShopProductAttrValue;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleDetailVO;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleFrontVO;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleSkuVO;
import com.sz.admin.shop_system.pojo.vo.ShopFlashSaleVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductAttrOption;
import com.sz.admin.shop_system.pojo.vo.ShopProductAttrVO;
import com.sz.admin.shop_system.service.ShopFlashSaleService;
import com.sz.admin.shop_system.service.ShopProductAttrService;
import com.sz.admin.shop_system.service.ShopProductAttrValueService;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopFlashSaleServiceImpl extends ServiceImpl<ShopFlashSaleMapper, ShopFlashSale> implements ShopFlashSaleService {

    private final ShopProductMapper lbProductMapper;
    private final ShopProductAttrMapper lbProductAttrMapper;
    private final ShopProductAttrService productAttrService;
    private final ShopProductAttrValueService productAttrValueService;
    private final ShopProductAttrValueMapper lbProductAttrValueMapper;

    @Override
    public PageResult<ShopFlashSaleVO> page(ShopFlashSaleDTO.ListQuery dto) {
        PageUtils.toPage(dto);
        try {
            List<ShopFlashSaleVO> list = mapper.selectMarketingPage(dto);
            return PageUtils.getPageResult(list);
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public ShopFlashSaleDetailVO detail(Long productId) {
        CommonResponseEnum.INVALID_ID.assertNull(productId);
        ShopProduct product = lbProductMapper.selectOneById(productId);
        CommonResponseEnum.INVALID.message("商品不存在").assertNull(product);

        ShopFlashSale existing = mapper.selectByProductId(productId);
        List<ShopProductAttr> productAttrs = loadProductAttrs(product.getId());
        List<ShopProductAttrValue> productAttrValues = productAttrValueService.getListByProductId(product.getId());
        return assembleDetailVO(product, existing, productAttrs, productAttrValues);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMarketing(ShopFlashSaleDTO.Update dto) {
        CommonResponseEnum.INVALID.message("营销配置不能为空").assertNull(dto);
        CommonResponseEnum.INVALID_ID.assertNull(dto.getProductId());

        ShopProduct product = lbProductMapper.selectOneById(dto.getProductId());
        CommonResponseEnum.INVALID.message("商品不存在").assertNull(product);

        updateProductMarketingFields(product, dto);

        ShopFlashSale existing = mapper.selectByProductId(dto.getProductId());
        if (!needsFlashSaleConfig(dto) && existing == null) {
            return;
        }

        LocalDateTime startTime = resolveDateTime(dto.getStartTime(), existing == null ? null : existing.getStartTime());
        LocalDateTime endTime = resolveDateTime(dto.getEndTime(), existing == null ? null : existing.getEndTime());
        BigDecimal flashPrice = dto.getFlashPrice() == null
                ? (existing == null ? product.getPrice() : existing.getFlashPrice())
                : dto.getFlashPrice();
        validateFlashSaleConfig(dto, existing, startTime, endTime, flashPrice);

        updateSkuFlashPrices(dto.getProductId(), dto.getSkuList(), flashPrice == null ? product.getPrice() : flashPrice);
        persistFlashSaleConfig(dto, existing, startTime, endTime, flashPrice);
    }

    @Override
    public List<ShopFlashSaleFrontVO> activeFrontList(Integer size) {
        int limit = size == null || size <= 0 ? 4 : Math.min(size, 8);
        LocalDateTime now = LocalDateTime.now();
        List<ShopFlashSaleFrontVO> rows = mapper.selectActiveFrontList(now, limit);
        rows.forEach(item -> fillFrontState(item, now));
        return rows;
    }

    @Override
    public ShopFlashSaleFrontVO frontDetail(Long productId) {
        if (productId == null) {
            return null;
        }
        ShopFlashSaleFrontVO vo = mapper.selectFrontDetailByProductId(productId);
        fillFrontState(vo, LocalDateTime.now());
        return vo;
    }

    @Override
    public ShopFlashSale getAvailableByProductId(Long productId, LocalDateTime now) {
        if (productId == null) {
            return null;
        }
        LocalDateTime current = now == null ? LocalDateTime.now() : now;
        return mapper.selectAvailableByProductId(productId, current);
    }

    private ShopFlashSaleDetailVO assembleDetailVO(ShopProduct product,
                                                 ShopFlashSale existing,
                                                 List<ShopProductAttr> productAttrs,
                                                 List<ShopProductAttrValue> productAttrValues) {
        ShopFlashSaleDetailVO vo = new ShopFlashSaleDetailVO();
        vo.setProductId(product.getId());
        vo.setProductName(product.getProductName());
        vo.setImage(product.getImage());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setBrowse(product.getBrowse());
        vo.setFictiBrowse(product.getFictiBrowse());
        vo.setSales(product.getSales());
        vo.setProductShowStatus(product.getIsShow());
        vo.setIsSeckill(product.getIsSeckill());
        vo.setActivity(product.getActivity());
        vo.setFlashStatus(existing == null ? 0 : existing.getStatus());
        vo.setFlashPrice(resolveBaseFlashPrice(product, existing));
        vo.setLimitPerUser(existing == null || existing.getLimitPerUser() == null ? 1 : existing.getLimitPerUser());
        vo.setReserveSeconds(existing == null || existing.getReserveSeconds() == null ? 900 : existing.getReserveSeconds());
        vo.setStartTime(existing == null ? null : existing.getStartTime());
        vo.setEndTime(existing == null ? null : existing.getEndTime());
        vo.setAttrList(buildAttrVOList(productAttrs, product));
        vo.setSkuList(buildSkuVOList(productAttrs, productAttrValues, vo.getFlashPrice()));
        return vo;
    }

    private void updateProductMarketingFields(ShopProduct product, ShopFlashSaleDTO.Update dto) {
        ShopProduct productUpdate = new ShopProduct();
        productUpdate.setId(product.getId());
        if (dto.getBrowse() != null) {
            productUpdate.setBrowse(Math.max(dto.getBrowse(), 0));
        }
        if (dto.getFictiBrowse() != null) {
            productUpdate.setFictiBrowse(Math.max(dto.getFictiBrowse(), 0));
        }
        if (dto.getSales() != null) {
            productUpdate.setSales(Math.max(dto.getSales(), 0));
        }
        if (dto.getProductShowStatus() != null) {
            productUpdate.setIsShow(dto.getProductShowStatus());
        }
        if (dto.getIsSeckill() != null) {
            productUpdate.setIsSeckill(dto.getIsSeckill());
        }
        if (dto.getActivity() != null) {
            productUpdate.setActivity(dto.getActivity());
        }
        lbProductMapper.update(productUpdate);
    }

    private boolean needsFlashSaleConfig(ShopFlashSaleDTO.Update dto) {
        return (dto.getIsSeckill() != null && dto.getIsSeckill() == 1)
                || "1".equals(dto.getActivity())
                || dto.getFlashPrice() != null
                || dto.getStartTime() != null
                || dto.getEndTime() != null
                || dto.getFlashStatus() != null
                || dto.getSkuList() != null;
    }

    private void validateFlashSaleConfig(ShopFlashSaleDTO.Update dto,
                                         ShopFlashSale existing,
                                         LocalDateTime startTime,
                                         LocalDateTime endTime,
                                         BigDecimal flashPrice) {
        boolean timeRequired = existing == null
                || dto.getFlashStatus() != null
                || dto.getFlashPrice() != null
                || dto.getStartTime() != null
                || dto.getEndTime() != null;
        CommonResponseEnum.INVALID.message("秒杀开始时间不能为空")
                .assertTrue(timeRequired && startTime == null);
        CommonResponseEnum.INVALID.message("秒杀结束时间不能为空")
                .assertTrue(timeRequired && endTime == null);
        CommonResponseEnum.INVALID.message("秒杀结束时间必须晚于开始时间")
                .assertTrue(startTime != null && endTime != null && !endTime.isAfter(startTime));
        CommonResponseEnum.INVALID.message("秒杀价格必须大于0")
                .assertTrue(flashPrice == null || flashPrice.compareTo(BigDecimal.ZERO) <= 0);
    }

    private void persistFlashSaleConfig(ShopFlashSaleDTO.Update dto,
                                        ShopFlashSale existing,
                                        LocalDateTime startTime,
                                        LocalDateTime endTime,
                                        BigDecimal flashPrice) {
        if (existing == null) {
            ShopFlashSale flashSale = new ShopFlashSale()
                    .setProductId(dto.getProductId())
                    .setFlashPrice(flashPrice)
                    .setLimitPerUser(dto.getLimitPerUser() == null ? 1 : Math.max(dto.getLimitPerUser(), 1))
                    .setReserveSeconds(dto.getReserveSeconds() == null ? 900 : Math.max(dto.getReserveSeconds(), 60))
                    .setStatus(dto.getFlashStatus() == null ? 0 : dto.getFlashStatus())
                    .setSort(0)
                    .setStartTime(startTime)
                    .setEndTime(endTime)
                    .setIsDel(0);
            save(flashSale);
            return;
        }

        ShopFlashSale update = new ShopFlashSale();
        update.setId(existing.getId());
        update.setFlashPrice(flashPrice);
        update.setLimitPerUser(dto.getLimitPerUser() == null ? existing.getLimitPerUser() : Math.max(dto.getLimitPerUser(), 1));
        update.setReserveSeconds(dto.getReserveSeconds() == null ? existing.getReserveSeconds() : Math.max(dto.getReserveSeconds(), 60));
        update.setStatus(dto.getFlashStatus() == null ? existing.getStatus() : dto.getFlashStatus());
        update.setStartTime(startTime);
        update.setEndTime(endTime);
        updateById(update);
    }

    private void updateSkuFlashPrices(Long productId, List<ShopFlashSaleDTO.Sku> skuList, BigDecimal defaultFlashPrice) {
        if (skuList == null || skuList.isEmpty()) {
            return;
        }
        List<ShopProductAttrValue> productSkus = productAttrValueService.getListByProductId(productId);
        Map<Long, ShopProductAttrValue> skuMap = productSkus.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(ShopProductAttrValue::getId, item -> item, (a, b) -> a));

        for (ShopFlashSaleDTO.Sku skuDTO : skuList) {
            CommonResponseEnum.INVALID.message("SKU配置不能为空").assertNull(skuDTO);
            CommonResponseEnum.INVALID_ID.assertNull(skuDTO.getSkuId());
            ShopProductAttrValue sku = skuMap.get(skuDTO.getSkuId());
            CommonResponseEnum.INVALID.message("商品规格不存在").assertNull(sku);

            BigDecimal skuFlashPrice = skuDTO.getFlashPrice() == null ? defaultFlashPrice : skuDTO.getFlashPrice();
            CommonResponseEnum.INVALID.message("SKU秒杀价格必须大于0")
                    .assertTrue(skuFlashPrice == null || skuFlashPrice.compareTo(BigDecimal.ZERO) <= 0);

            ShopProductAttrValue update = new ShopProductAttrValue();
            update.setId(sku.getId());
            update.setFlashPrice(skuFlashPrice);
            lbProductAttrValueMapper.update(update);
        }
    }

    private List<ShopProductAttr> loadProductAttrs(Long productId) {
        List<ShopProductAttr> productAttrs = productAttrService.getListByProductIdAndType(productId, Constant.PRODUCT_TYPE_NORMAL);
        if (productAttrs == null || productAttrs.isEmpty()) {
            productAttrs = lbProductAttrMapper.selectByProductId(productId);
        }
        return productAttrs == null ? new ArrayList<>() : productAttrs;
    }

    private BigDecimal resolveBaseFlashPrice(ShopProduct product, ShopFlashSale existing) {
        if (existing != null && existing.getFlashPrice() != null) {
            return existing.getFlashPrice();
        }
        return product.getPrice();
    }

    private List<ShopFlashSaleSkuVO> buildSkuVOList(List<ShopProductAttr> productAttrs,
                                                  List<ShopProductAttrValue> productAttrValues,
                                                  BigDecimal defaultFlashPrice) {
        if (productAttrValues == null || productAttrValues.isEmpty()) {
            return new ArrayList<>();
        }
        return productAttrValues.stream().map(item -> {
            ShopFlashSaleSkuVO vo = new ShopFlashSaleSkuVO();
            vo.setSkuId(item.getId());
            vo.setSku(item.getSku());
            vo.setAttrValue(buildSkuAttrValue(item.getSku(), productAttrs));
            vo.setStock(item.getStock());
            vo.setPrice(item.getPrice());
            vo.setFlashPrice(item.getFlashPrice() == null ? defaultFlashPrice : item.getFlashPrice());
            vo.setIsDefault(item.getIsDefault());
            vo.setImage(item.getImage());
            return vo;
        }).collect(Collectors.toList());
    }

    private String buildSkuAttrValue(String sku, List<ShopProductAttr> productAttrs) {
        if (sku == null || sku.isBlank()) {
            return "";
        }
        if (productAttrs == null || productAttrs.isEmpty()) {
            return sku;
        }
        String[] skuParts = sku.split(",");
        Map<String, String> attrValueMap = new LinkedHashMap<>();
        for (int i = 0; i < productAttrs.size() && i < skuParts.length; i++) {
            String attrName = productAttrs.get(i).getAttrName();
            String attrValue = skuParts[i] == null ? "" : skuParts[i].trim();
            if (attrName != null && !attrName.isBlank() && !attrValue.isBlank()) {
                attrValueMap.put(attrName, attrValue);
            }
        }
        if (attrValueMap.isEmpty()) {
            return sku;
        }
        return JSONUtil.toJsonStr(attrValueMap);
    }

    private List<ShopProductAttrVO> buildAttrVOList(List<ShopProductAttr> productAttrs, ShopProduct product) {
        List<ShopProductAttrVO> attrVOList = new ArrayList<>();
        for (ShopProductAttr attr : productAttrs) {
            ShopProductAttrVO attrVO = new ShopProductAttrVO();
            attrVO.setId(attr.getId());
            attrVO.setProductId(attr.getProductId());
            attrVO.setAttrName(attr.getAttrName());
            attrVO.setIsDel(attr.getIsDel());

            List<ShopProductAttrOption> optionList = new ArrayList<>();
            String attrValues = attr.getAttrValues();
            if (attrValues != null && !attrValues.isBlank()) {
                for (String option : attrValues.split(",")) {
                    String optionName = option == null ? "" : option.trim();
                    if (optionName.isBlank()) {
                        continue;
                    }
                    ShopProductAttrOption attrOption = new ShopProductAttrOption();
                    attrOption.setAttrId(attr.getId());
                    attrOption.setProductId(product.getId());
                    attrOption.setOptionName(optionName);
                    optionList.add(attrOption);
                }
            }
            attrVO.setOptionList(optionList);
            attrVOList.add(attrVO);
        }
        return attrVOList;
    }

    private static LocalDateTime resolveDateTime(LocalDateTime candidate, LocalDateTime fallback) {
        return candidate == null ? fallback : candidate;
    }

    private static void fillFrontState(ShopFlashSaleFrontVO vo, LocalDateTime now) {
        if (vo == null) {
            return;
        }
        if (vo.getStatus() == null || vo.getStatus() != 1) {
            vo.setCanRush(Boolean.FALSE);
            vo.setStatusText("已下架");
            vo.setRemainingSeconds(0L);
            return;
        }
        if (vo.getStartTime() != null && now.isBefore(vo.getStartTime())) {
            vo.setCanRush(Boolean.FALSE);
            vo.setStatusText("即将开始");
            vo.setRemainingSeconds(Duration.between(now, vo.getStartTime()).getSeconds());
            return;
        }
        if (vo.getEndTime() != null && now.isAfter(vo.getEndTime())) {
            vo.setCanRush(Boolean.FALSE);
            vo.setStatusText("已结束");
            vo.setRemainingSeconds(0L);
            return;
        }
        vo.setCanRush(Boolean.TRUE);
        vo.setStatusText("抢购中");
        if (vo.getEndTime() != null) {
            vo.setRemainingSeconds(Math.max(Duration.between(now, vo.getEndTime()).getSeconds(), 0L));
        } else {
            vo.setRemainingSeconds(0L);
        }
    }
}
