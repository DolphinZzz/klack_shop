package com.sz.admin.shop_system.agent.service.impl;

import com.sz.admin.shop_system.agent.pojo.dto.AgentMallDTO;
import com.sz.admin.shop_system.agent.pojo.vo.AgentMallVO;
import com.sz.admin.shop_system.agent.service.AgentMallService;
import com.sz.admin.shop_system.enums.ShopOrderAfterSaleStatusEnum;
import com.sz.admin.shop_system.enums.ShopOrderAfterSaleTypeEnum;
import com.sz.admin.shop_system.mapper.ShopExpressInfoMapper;
import com.sz.admin.shop_system.mapper.ShopExpressTemplateMapper;
import com.sz.admin.shop_system.mapper.ShopOrderAfterSaleMapper;
import com.sz.admin.shop_system.mapper.ShopOrderMapper;
import com.sz.admin.shop_system.mapper.ShopProductMapper;
import com.sz.admin.shop_system.mapper.ShopUserAddressMapper;
import com.sz.admin.shop_system.pojo.dto.ShopOrderDTO;
import com.sz.admin.shop_system.pojo.po.ShopCategory;
import com.sz.admin.shop_system.pojo.po.ShopExpress;
import com.sz.admin.shop_system.pojo.po.ShopExpressInfo;
import com.sz.admin.shop_system.pojo.po.ShopExpressTemplate;
import com.sz.admin.shop_system.pojo.po.ShopFlashSale;
import com.sz.admin.shop_system.pojo.po.ShopOrder;
import com.sz.admin.shop_system.pojo.po.ShopOrderAfterSale;
import com.sz.admin.shop_system.pojo.po.ShopProduct;
import com.sz.admin.shop_system.pojo.po.ShopProductAttrValue;
import com.sz.admin.shop_system.pojo.po.ShopUserAddress;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderDetailVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderVO;
import com.sz.admin.shop_system.service.ShopCategoryService;
import com.sz.admin.shop_system.service.ShopExpressService;
import com.sz.admin.shop_system.service.ShopFlashSaleService;
import com.sz.admin.shop_system.service.ShopOrderService;
import com.sz.admin.shop_system.service.ShopProductAttrValueService;
import com.sz.admin.shop_system.service.ShopProductService;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.enums.CommonResponseEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 智能客服 Agent 内网商城数据服务实现
 */
@Service
@RequiredArgsConstructor
public class AgentMallServiceImpl implements AgentMallService {

    private static final int DEFAULT_RECOMMEND_LIMIT = 5;
    private static final int DEFAULT_SEARCH_LIMIT = 6;
    private static final int MAX_LIST_LIMIT = 20;

    private final ShopOrderService shopOrderService;
    private final ShopOrderMapper shopOrderMapper;
    private final ShopProductMapper shopProductMapper;
    private final ShopProductService shopProductService;
    private final ShopProductAttrValueService shopProductAttrValueService;
    private final ShopFlashSaleService shopFlashSaleService;
    private final ShopCategoryService shopCategoryService;
    private final ShopUserAddressMapper shopUserAddressMapper;
    private final ShopExpressInfoMapper shopExpressInfoMapper;
    private final ShopExpressTemplateMapper shopExpressTemplateMapper;
    private final ShopExpressService shopExpressService;
    private final ShopOrderAfterSaleMapper shopOrderAfterSaleMapper;

    @Override
    public List<AgentMallVO.ProductSummary> recommendProducts(String sort, Integer categoryId, Integer limit) {
        String normalizedSort = normalizeRecommendSort(sort);
        int effectiveLimit = normalizeLimit(limit, DEFAULT_RECOMMEND_LIMIT);
        List<Integer> categoryIdList = resolveCategoryIds(categoryId);
        List<ShopProduct> products = shopProductMapper.selectAgentRecommendProducts(normalizedSort, categoryIdList, effectiveLimit);
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        return products.stream().map(this::toProductSummary).collect(Collectors.toList());
    }

    @Override
    public List<AgentMallVO.ProductSummary> searchProducts(String keyword,
                                                           Integer categoryId,
                                                           BigDecimal minPrice,
                                                           BigDecimal maxPrice,
                                                           Integer limit) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        int effectiveLimit = normalizeLimit(limit, DEFAULT_SEARCH_LIMIT);
        List<Integer> categoryIdList = resolveCategoryIds(categoryId);
        List<ShopProduct> products = shopProductMapper.selectAgentSearchProducts(
                normalizedKeyword, categoryIdList, minPrice, maxPrice, effectiveLimit);
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        return products.stream().map(this::toProductSummary).collect(Collectors.toList());
    }

    @Override
    public AgentMallVO.ProductDetail getProductDetail(Long productId) {
        CommonResponseEnum.INVALID_ID.assertTrue(productId == null || productId <= 0);
        ShopProduct product = shopProductService.getById(productId);
        CommonResponseEnum.INVALID.message("商品不存在").assertTrue(product == null || (product.getIsDel() != null && product.getIsDel() == 1));

        List<ShopProductAttrValue> skuEntities = shopProductAttrValueService.getListByProductId(product.getId());
        ShopFlashSale activeFlashSale = shopFlashSaleService.getAvailableByProductId(product.getId(), LocalDateTime.now());
        return toProductDetail(product, skuEntities, activeFlashSale);
    }

    @Override
    public List<AgentMallVO.UserAddress> listUserAddresses(Long userId) {
        CommonResponseEnum.INVALID_TOKEN.assertTrue(userId == null || userId <= 0);
        List<ShopUserAddress> addresses = shopUserAddressMapper.selectActiveByUid(userId);
        if (addresses == null || addresses.isEmpty()) {
            return Collections.emptyList();
        }
        return addresses.stream()
                .sorted(Comparator
                        .comparingInt((ShopUserAddress a) -> a.getIsDefault() != null && a.getIsDefault() == 1 ? 0 : 1)
                        .thenComparing(ShopUserAddress::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ShopUserAddress::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toUserAddress)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<ShopFrontOrderVO> listUserOrders(Long userId, String tab, String keyword, Integer page, Integer limit) {
        CommonResponseEnum.INVALID_TOKEN.assertTrue(userId == null || userId <= 0);
        int pageNum = page == null || page < 1 ? 1 : page;
        int limitNum = normalizeLimit(limit, 5);
        String normalizedTab = normalizeOrderTab(tab);

        ShopOrderDTO.FrontList query = new ShopOrderDTO.FrontList();
        query.setTab(normalizedTab);
        query.setKeyword(keyword);
        query.setPage(pageNum);
        query.setLimit(limitNum);
        return shopOrderService.frontPageForUser(userId, query);
    }

    @Override
    public AgentMallVO.OrderDetail getOrderDetail(Long userId, String orderNo) {
        CommonResponseEnum.INVALID_TOKEN.assertTrue(userId == null || userId <= 0);
        CommonResponseEnum.INVALID.message("订单号不能为空").assertTrue(orderNo == null || orderNo.isBlank());

        ShopFrontOrderDetailVO frontDetail = shopOrderService.frontDetailForUser(userId, orderNo);
        ShopOrder order = shopOrderMapper.selectByOrderNoAndUid(orderNo, userId);

        AgentMallVO.ShipmentInfo shipmentInfo = null;
        AgentMallVO.AfterSaleSummary afterSaleSummary = null;
        if (order != null && order.getId() != null) {
            ShopExpressInfo expressInfo = shopExpressInfoMapper.selectLatestByOid(order.getId());
            shipmentInfo = buildShipmentInfo(expressInfo, frontDetail);
            ShopOrderAfterSale afterSale = shopOrderAfterSaleMapper.selectActiveByOrderId(order.getId());
            if (afterSale != null) {
                afterSaleSummary = toAfterSaleSummary(afterSale);
            }
        } else {
            shipmentInfo = buildShipmentInfo(null, frontDetail);
        }

        return assembleOrderDetail(frontDetail, order, shipmentInfo, afterSaleSummary);
    }

    @Override
    public List<AgentMallVO.AfterSaleSummary> listUserAfterSales(Long userId, String orderNo, String serviceNo, Integer limit) {
        CommonResponseEnum.INVALID_TOKEN.assertTrue(userId == null || userId <= 0);
        int effectiveLimit = normalizeLimit(limit, 10);
        String normalizedOrderNo = orderNo == null || orderNo.isBlank() ? null : orderNo.trim();
        String normalizedServiceNo = serviceNo == null || serviceNo.isBlank() ? null : serviceNo.trim();
        List<ShopOrderAfterSale> list = shopOrderAfterSaleMapper.selectAgentListByUid(
                userId, normalizedOrderNo, normalizedServiceNo, effectiveLimit);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(this::toAfterSaleSummary).collect(Collectors.toList());
    }

    @Override
    public AgentMallVO.DirectOrderResult directCreateOrder(Long userId, AgentMallDTO.DirectCreateOrder dto) {
        return shopOrderService.directCreateForAgent(userId, dto);
    }

    private List<Integer> resolveCategoryIds(Integer categoryId) {
        if (categoryId == null || categoryId <= 0) {
            return Collections.emptyList();
        }
        List<Integer> ids = new ArrayList<>();
        ids.add(categoryId);
        List<ShopCategory> children = shopCategoryService.getByPIds(List.of(categoryId));
        if (children != null) {
            children.stream()
                    .map(ShopCategory::getId)
                    .filter(Objects::nonNull)
                    .map(Long::intValue)
                    .forEach(ids::add);
        }
        return ids.stream().distinct().collect(Collectors.toList());
    }

    private AgentMallVO.ProductSummary toProductSummary(ShopProduct product) {
        AgentMallVO.ProductSummary vo = new AgentMallVO.ProductSummary();
        vo.setId(product.getId());
        vo.setProductName(product.getProductName());
        vo.setProductInfo(product.getProductInfo());
        vo.setImage(product.getImage());
        vo.setCateId(product.getCateId());
        vo.setPrice(product.getPrice());
        vo.setOtPrice(product.getOtPrice());
        vo.setStock(product.getStock());
        vo.setSales(product.getSales());
        vo.setBrowse(product.getBrowse());
        vo.setIsHot(product.getIsHot());
        vo.setIsBest(product.getIsBest());
        vo.setIsNew(product.getIsNew());
        vo.setIsSeckill(product.getIsSeckill());
        vo.setIsPostage(product.getIsPostage());
        vo.setPostage(product.getPostage());
        vo.setUnitName(product.getUnitName());
        return vo;
    }

    private AgentMallVO.ProductDetail toProductDetail(ShopProduct product,
                                                      List<ShopProductAttrValue> skuEntities,
                                                      ShopFlashSale activeFlashSale) {
        AgentMallVO.ProductDetail detail = new AgentMallVO.ProductDetail();
        detail.setId(product.getId());
        detail.setProductName(product.getProductName());
        detail.setProductInfo(product.getProductInfo());
        detail.setKeyword(product.getKeyword());
        detail.setCateId(product.getCateId());
        detail.setImage(product.getImage());
        detail.setSliderImage(product.getSliderImage());
        detail.setPrice(product.getPrice());
        detail.setOtPrice(product.getOtPrice());
        detail.setStock(product.getStock());
        detail.setSales(product.getSales());
        detail.setBrowse(product.getBrowse());
        detail.setIsShow(product.getIsShow());
        detail.setIsDel(product.getIsDel());
        detail.setIsHot(product.getIsHot());
        detail.setIsBest(product.getIsBest());
        detail.setIsNew(product.getIsNew());
        detail.setIsSeckill(product.getIsSeckill());
        detail.setActiveFlashSale(activeFlashSale != null);
        detail.setFlashSaleId(activeFlashSale == null ? null : activeFlashSale.getId());
        detail.setIsPostage(product.getIsPostage());
        detail.setPostage(product.getPostage());
        detail.setUnitName(product.getUnitName());
        detail.setDesc(product.getDesc());
        List<AgentMallVO.ProductSku> skus = skuEntities == null ? Collections.emptyList()
                : skuEntities.stream().map(this::toProductSku).collect(Collectors.toList());
        detail.setSkus(skus);
        return detail;
    }

    private AgentMallVO.ProductSku toProductSku(ShopProductAttrValue sku) {
        AgentMallVO.ProductSku vo = new AgentMallVO.ProductSku();
        vo.setSkuId(sku.getId());
        vo.setSku(sku.getSku());
        vo.setPrice(sku.getPrice());
        vo.setOtPrice(sku.getOtPrice());
        vo.setFlashPrice(sku.getFlashPrice());
        vo.setStock(sku.getStock());
        vo.setImage(sku.getImage());
        vo.setIsDefault(sku.getIsDefault());
        return vo;
    }

    private AgentMallVO.UserAddress toUserAddress(ShopUserAddress address) {
        AgentMallVO.UserAddress vo = new AgentMallVO.UserAddress();
        vo.setId(address.getId());
        vo.setUid(address.getUid());
        vo.setRealName(address.getRealName());
        vo.setPhone(address.getPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetail(address.getDetail());
        vo.setFullAddress(joinAddress(address));
        vo.setPostCode(address.getPostCode());
        vo.setIsDefault(address.getIsDefault());
        return vo;
    }

    private AgentMallVO.ShipmentInfo buildShipmentInfo(ShopExpressInfo expressInfo, ShopFrontOrderDetailVO frontDetail) {
        if (expressInfo == null) {
            if (frontDetail.getExpressNo() == null || frontDetail.getExpressNo().isBlank()) {
                return null;
            }
            AgentMallVO.ShipmentInfo fallback = new AgentMallVO.ShipmentInfo();
            fallback.setExpressNo(frontDetail.getExpressNo());
            fallback.setExpressCompanyName(frontDetail.getExpressName());
            fallback.setRealName(frontDetail.getRealName());
            fallback.setUserPhone(frontDetail.getUserPhone());
            fallback.setUserAddress(frontDetail.getUserAddress());
            return fallback;
        }

        AgentMallVO.ShipmentInfo shipment = new AgentMallVO.ShipmentInfo();
        shipment.setExpressNo(expressInfo.getExpressNo() != null ? expressInfo.getExpressNo() : frontDetail.getExpressNo());
        shipment.setExpressTmpId(expressInfo.getExpressTmpId());
        shipment.setStatus(expressInfo.getStatus());
        shipment.setStatusText(expressInfo.getStatus() != null && expressInfo.getStatus() == 1 ? "已发货" : "待发货");
        shipment.setCreateTime(expressInfo.getCreateTime());
        shipment.setRealName(expressInfo.getRealName());
        shipment.setUserPhone(expressInfo.getUserPhone());
        shipment.setUserAddress(expressInfo.getUserAddress());

        String companyName = frontDetail.getExpressName();
        if (expressInfo.getExpressTmpId() != null) {
            ShopExpressTemplate template = shopExpressTemplateMapper.selectActiveById(expressInfo.getExpressTmpId());
            if (template != null) {
                shipment.setExpressTemplateName(template.getName());
                if ((companyName == null || companyName.isBlank()) && template.getExpressId() != null) {
                    ShopExpress express = shopExpressService.getById(template.getExpressId());
                    if (express != null) {
                        companyName = express.getExpressName();
                    }
                }
            }
        }
        shipment.setExpressCompanyName(companyName);
        return shipment;
    }

    private AgentMallVO.OrderDetail assembleOrderDetail(ShopFrontOrderDetailVO frontDetail,
                                                        ShopOrder order,
                                                        AgentMallVO.ShipmentInfo shipmentInfo,
                                                        AgentMallVO.AfterSaleSummary afterSaleSummary) {
        AgentMallVO.OrderDetail detail = new AgentMallVO.OrderDetail();
        detail.setOrderNo(frontDetail.getOrderNo());
        detail.setPaid(frontDetail.getPaid());
        detail.setStatus(frontDetail.getStatus());
        detail.setStatusDesc(resolveOrderStatusDesc(frontDetail, order));
        detail.setDisplayTab(frontDetail.getDisplayTab());
        detail.setDisplayStatus(frontDetail.getDisplayStatus());
        detail.setOrderType(frontDetail.getOrderType());
        detail.setOrderTypeLabel(frontDetail.getOrderTypeLabel());
        detail.setCreateTime(frontDetail.getCreateTime());
        detail.setPayTime(frontDetail.getPayTime());
        detail.setPayPrice(frontDetail.getPayPrice());
        detail.setFreightTotalFee(frontDetail.getFreightTotalFee());
        detail.setTotalNum(frontDetail.getTotalNum());
        detail.setRealName(frontDetail.getRealName());
        detail.setUserPhone(frontDetail.getUserPhone());
        detail.setUserAddress(frontDetail.getUserAddress());
        detail.setPayType(frontDetail.getPayType());
        detail.setExpressNo(frontDetail.getExpressNo());
        detail.setExpressName(frontDetail.getExpressName());
        detail.setCanPay(frontDetail.getCanPay());
        detail.setCanConfirmReceipt(frontDetail.getCanConfirmReceipt());
        detail.setCanEvaluate(frontDetail.getCanEvaluate());
        detail.setCanApplyAfterSale(frontDetail.getCanApplyAfterSale());
        detail.setAfterSaleId(frontDetail.getAfterSaleId());
        detail.setAfterSaleStatus(frontDetail.getAfterSaleStatus());
        detail.setAfterSaleStatusText(frontDetail.getAfterSaleStatusText());
        detail.setOrderInfoList(frontDetail.getOrderInfoList());
        detail.setOrderStatusList(frontDetail.getOrderStatusList());
        detail.setShipment(shipmentInfo);
        detail.setAfterSale(afterSaleSummary);
        detail.setTrackingSupported(Boolean.FALSE);
        return detail;
    }

    private AgentMallVO.AfterSaleSummary toAfterSaleSummary(ShopOrderAfterSale afterSale) {
        AgentMallVO.AfterSaleSummary vo = new AgentMallVO.AfterSaleSummary();
        vo.setId(afterSale.getId());
        vo.setServiceNo(afterSale.getServiceNo());
        vo.setOrderId(afterSale.getOrderId());
        vo.setOrderNo(afterSale.getOrderNo());
        vo.setAfterSaleType(afterSale.getAfterSaleType());
        vo.setAfterSaleTypeText(ShopOrderAfterSaleTypeEnum.getDesc(afterSale.getAfterSaleType()));
        vo.setStatus(afterSale.getStatus());
        vo.setStatusText(ShopOrderAfterSaleStatusEnum.getDesc(afterSale.getStatus()));
        vo.setApplyAmount(afterSale.getApplyAmount());
        vo.setReason(afterSale.getReason());
        vo.setUserRemark(afterSale.getUserRemark());
        vo.setAuditRemark(afterSale.getAuditRemark());
        vo.setReceiveRemark(afterSale.getReceiveRemark());
        vo.setReturnExpressName(afterSale.getReturnExpressName());
        vo.setReturnExpressNo(afterSale.getReturnExpressNo());
        vo.setReturnRemark(afterSale.getReturnRemark());
        vo.setAuditTime(afterSale.getAuditTime());
        vo.setReturnTime(afterSale.getReturnTime());
        vo.setReceiveTime(afterSale.getReceiveTime());
        vo.setRefundTime(afterSale.getRefundTime());
        vo.setCreateTime(afterSale.getCreateTime());
        return vo;
    }

    private static String normalizeRecommendSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "hot";
        }
        String normalized = sort.trim().toLowerCase(Locale.ROOT);
        if ("sales".equals(normalized) || "browse".equals(normalized)) {
            return normalized;
        }
        return "hot";
    }

    private static String normalizeOrderTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return "all";
        }
        String normalized = tab.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "unpaid" -> "pending_payment";
            case "wait_deliver" -> "pending_shipment";
            case "wait_receive" -> "pending_receipt";
            case "wait_evaluate", "pending_evaluate" -> "pending_review";
            default -> normalized;
        };
    }

    private static int normalizeLimit(Integer limit, int defaultLimit) {
        if (limit == null || limit <= 0) {
            return defaultLimit;
        }
        return Math.min(limit, MAX_LIST_LIMIT);
    }

    private static String resolveOrderStatusDesc(ShopFrontOrderDetailVO frontDetail, ShopOrder order) {
        if (order != null) {
            Integer refundStatus = order.getRefundStatus();
            if (Integer.valueOf(2).equals(refundStatus)) {
                return "已退款";
            }
            if (Integer.valueOf(1).equals(refundStatus)) {
                return "退款中";
            }
        }
        Integer paid = frontDetail.getPaid();
        Integer status = frontDetail.getStatus();
        if ((paid != null && paid == 2) || (status != null && status == 5)) {
            return "已取消";
        }
        if (paid == null || paid == 0) {
            return "待付款";
        }
        if (paid == 1) {
            int statusCode = status == null ? 0 : status;
            return switch (statusCode) {
                case 0 -> "待发货";
                case 1 -> "已发货";
                case 2 -> "待评价";
                case 3 -> "已完成";
                default -> frontDetail.getDisplayStatus();
            };
        }
        return frontDetail.getDisplayStatus();
    }

    private static String joinAddress(ShopUserAddress address) {
        List<String> parts = new ArrayList<>();
        if (address.getProvince() != null && !address.getProvince().isBlank()) parts.add(address.getProvince().trim());
        if (address.getCity() != null && !address.getCity().isBlank()) parts.add(address.getCity().trim());
        if (address.getDistrict() != null && !address.getDistrict().isBlank()) parts.add(address.getDistrict().trim());
        if (address.getDetail() != null && !address.getDetail().isBlank()) parts.add(address.getDetail().trim());
        return String.join(" ", parts);
    }
}
