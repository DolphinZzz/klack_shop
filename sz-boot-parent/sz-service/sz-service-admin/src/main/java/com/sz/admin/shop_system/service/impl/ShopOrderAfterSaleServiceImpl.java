package com.sz.admin.shop_system.service.impl;

import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.constants.ShopOrderAfterSaleConstant;
import com.sz.admin.shop_system.constants.ShopOrderStatusConstant;
import com.sz.admin.shop_system.constants.ShopPaymentConstant;
import com.sz.admin.shop_system.enums.ShopOrderAfterSaleStatusEnum;
import com.sz.admin.shop_system.enums.ShopOrderAfterSaleTypeEnum;
import com.sz.admin.shop_system.mapper.ShopOrderAfterSaleMapper;
import com.sz.admin.shop_system.mapper.ShopOrderMapper;
import com.sz.admin.shop_system.pojo.dto.ShopOrderAfterSaleDTO;
import com.sz.admin.shop_system.pojo.po.ShopOrder;
import com.sz.admin.shop_system.pojo.po.ShopOrderAfterSale;
import com.sz.admin.shop_system.pojo.po.ShopOrderStatus;
import com.sz.admin.shop_system.pojo.po.ShopUser;
import com.sz.admin.shop_system.pojo.po.ShopUserBill;
import com.sz.admin.shop_system.pojo.po.ShopUserIntegralBill;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderAfterSaleVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderProductVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderAfterSaleVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductCartAndSkuVO;
import com.sz.admin.shop_system.service.ShopOrderAfterSaleService;
import com.sz.admin.shop_system.service.ShopOrderStatusService;
import com.sz.admin.shop_system.service.ShopUserBillService;
import com.sz.admin.shop_system.service.ShopUserIntegralBillService;
import com.sz.admin.shop_system.service.ShopUserService;
import com.sz.admin.shop_system.util.SerialNumberGenerator;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单售后 服务实现类
 * <p>
 * 架构分层：业务编排 -> 数据校验与组装（build/assemble） -> 数据库落库（persist/update）。
 */
@Service
@RequiredArgsConstructor
public class ShopOrderAfterSaleServiceImpl extends ServiceImpl<ShopOrderAfterSaleMapper, ShopOrderAfterSale> implements ShopOrderAfterSaleService {

    private final ShopOrderAfterSaleMapper shopOrderAfterSaleMapper;
    private final ShopOrderMapper shopOrderMapper;
    private final ShopOrderStatusService shopOrderStatusService;
    private final ShopUserService shopUserService;
    private final ShopUserBillService shopUserBillService;
    private final ShopUserIntegralBillService shopUserIntegralBillService;

    // ==================== 业务编排层 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void frontApply(Long userId, ShopOrderAfterSaleDTO.FrontApply dto) {
        validateApplyRequest(userId, dto);

        ShopOrder order = requireFrontOrder(dto.getOrderNo().trim(), userId);
        validateOrderEligibleForAfterSale(order);

        String reason = dto.getReason() == null ? "" : dto.getReason().trim();
        BigDecimal applyAmount = resolveValidatedApplyAmount(order, dto.getApplyAmount());

        ShopOrderAfterSale afterSale = buildAfterSaleEntity(userId, order, dto, reason, applyAmount);
        persistAfterSaleApply(afterSale, order, applyAmount, reason);
    }

    @Override
    public PageResult<ShopFrontOrderAfterSaleVO> frontPage(Long userId, ShopOrderAfterSaleDTO.FrontList dto) {
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);
        ShopOrderAfterSaleDTO.FrontList query = normalizeFrontQuery(dto);

        PageResult<ShopFrontOrderAfterSaleVO> pageResult;
        try {
            PageUtils.toPage(query);
            List<ShopFrontOrderAfterSaleVO> rows = shopOrderAfterSaleMapper.selectFrontPageList(userId, query);
            pageResult = PageUtils.getPageResult(rows);
        } finally {
            PageHelper.clearPage();
        }

        pageResult.getRows().forEach(this::enrichFrontSummaryWithProducts);
        return pageResult;
    }

    @Override
    public ShopFrontOrderAfterSaleVO frontDetail(Long userId, Long id) {
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);
        CommonResponseEnum.INVALID_ID.assertNull(id);

        ShopOrderAfterSale afterSale = requireFrontAfterSale(id, userId);
        return assembleFrontDetail(afterSale);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void frontSubmitReturn(Long userId, ShopOrderAfterSaleDTO.FrontReturn dto) {
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);
        CommonResponseEnum.INVALID.message("退货物流参数不能为空").assertNull(dto);
        CommonResponseEnum.INVALID_ID.assertNull(dto.getId());

        ShopOrderAfterSale afterSale = requireFrontAfterSale(dto.getId(), userId);
        CommonResponseEnum.INVALID.message("当前售后单不需要填写退货物流")
                .assertTrue(!canFillReturnExpress(afterSale.getAfterSaleType(), afterSale.getStatus()));

        String expressName = dto.getReturnExpressName() == null ? "" : dto.getReturnExpressName().trim();
        String expressNo = dto.getReturnExpressNo() == null ? "" : dto.getReturnExpressNo().trim();
        CommonResponseEnum.INVALID.message("请填写退货快递公司").assertTrue(expressName.isBlank());
        CommonResponseEnum.INVALID.message("请填写退货快递单号").assertTrue(expressNo.isBlank());

        ShopOrderAfterSale updateEntity = buildReturnUpdateEntity(afterSale.getId(), expressName, expressNo, dto.getReturnRemark());
        persistReturnSubmission(afterSale, updateEntity, expressName, expressNo);
    }

    @Override
    public PageResult<ShopOrderAfterSaleVO> page(ShopOrderAfterSaleDTO.ListQuery dto) {
        ShopOrderAfterSaleDTO.ListQuery query = normalizeAdminQuery(dto);

        PageResult<ShopOrderAfterSaleVO> pageResult;
        try {
            PageUtils.toPage(query);
            List<ShopOrderAfterSaleVO> rows = shopOrderAfterSaleMapper.selectAdminPageList(query);
            pageResult = PageUtils.getPageResult(rows);
        } finally {
            PageHelper.clearPage();
        }

        pageResult.getRows().forEach(this::fillAdminSummary);
        return pageResult;
    }

    @Override
    public ShopOrderAfterSaleVO detail(Long id) {
        CommonResponseEnum.INVALID_ID.assertNull(id);
        ShopOrderAfterSale afterSale = requireAfterSale(id);
        return assembleAdminDetail(afterSale);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(ShopOrderAfterSaleDTO.Audit dto) {
        CommonResponseEnum.INVALID.message("审核参数不能为空").assertNull(dto);
        CommonResponseEnum.INVALID_ID.assertNull(dto.getId());
        CommonResponseEnum.INVALID.message("请确认审核结果").assertNull(dto.getApproved());

        ShopOrderAfterSale afterSale = requireAfterSale(dto.getId());
        CommonResponseEnum.INVALID.message("当前售后单已处理，请刷新后重试")
                .assertTrue(afterSale.getStatus() == null || afterSale.getStatus() != ShopOrderAfterSaleConstant.STATUS_WAIT_AUDIT);

        ShopOrder order = loadOrder(afterSale.getOrderId());
        String auditRemark = dto.getAuditRemark() == null ? null : dto.getAuditRemark().trim();

        if (Boolean.TRUE.equals(dto.getApproved())) {
            if (afterSale.getAfterSaleType() != null && afterSale.getAfterSaleType() == ShopOrderAfterSaleConstant.TYPE_REFUND_ONLY) {
                completeRefund(afterSale, order, auditRemark, null);
                return;
            }
            ShopOrderAfterSale approveUpdate = buildAuditUpdateEntity(afterSale.getId(), ShopOrderAfterSaleConstant.STATUS_WAIT_BUYER_RETURN, auditRemark);
            persistAuditApprovedForReturn(order, afterSale, approveUpdate);
            return;
        }

        ShopOrderAfterSale rejectUpdate = buildAuditUpdateEntity(afterSale.getId(), ShopOrderAfterSaleConstant.STATUS_REJECTED, auditRemark);
        persistAuditRejected(order, rejectUpdate, auditRemark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(ShopOrderAfterSaleDTO.Receive dto) {
        CommonResponseEnum.INVALID.message("收货参数不能为空").assertNull(dto);
        CommonResponseEnum.INVALID_ID.assertNull(dto.getId());

        ShopOrderAfterSale afterSale = requireAfterSale(dto.getId());
        CommonResponseEnum.INVALID.message("当前售后单未进入待收货状态")
                .assertTrue(afterSale.getStatus() == null || afterSale.getStatus() != ShopOrderAfterSaleConstant.STATUS_WAIT_MERCHANT_RECEIVE);

        ShopOrder order = loadOrder(afterSale.getOrderId());
        String receiveRemark = dto.getReceiveRemark() == null ? null : dto.getReceiveRemark().trim();
        completeRefund(afterSale, order, afterSale.getAuditRemark(), receiveRemark);
    }

    // ==================== 校验与数据组装层 ====================

    private void validateApplyRequest(Long userId, ShopOrderAfterSaleDTO.FrontApply dto) {
        CommonResponseEnum.INVALID_TOKEN.assertNull(userId);
        CommonResponseEnum.INVALID.message("售后申请不能为空").assertNull(dto);
        CommonResponseEnum.INVALID.message("订单号不能为空")
                .assertTrue(dto.getOrderNo() == null || dto.getOrderNo().isBlank());
        CommonResponseEnum.INVALID.message("售后类型无效")
                .assertTrue(dto.getAfterSaleType() == null || (dto.getAfterSaleType() != ShopOrderAfterSaleConstant.TYPE_REFUND_ONLY
                        && dto.getAfterSaleType() != ShopOrderAfterSaleConstant.TYPE_RETURN_REFUND));

        String reason = dto.getReason() == null ? "" : dto.getReason().trim();
        CommonResponseEnum.INVALID.message("请输入售后原因").assertTrue(reason.isBlank());
        CommonResponseEnum.INVALID.message("售后原因不能超过255字").assertTrue(reason.length() > 255);
    }

    private void validateOrderEligibleForAfterSale(ShopOrder order) {
        CommonResponseEnum.INVALID.message("当前订单暂不支持申请售后")
                .assertTrue(order.getPaid() == null || order.getPaid() != 1 || order.getStatus() == null || (order.getStatus() != 2 && order.getStatus() != 3));

        ShopOrderAfterSale exists = shopOrderAfterSaleMapper.selectActiveByOrderId(order.getId());
        CommonResponseEnum.INVALID.message("该订单已申请过售后，请勿重复提交").assertTrue(exists != null);
    }

    private BigDecimal resolveValidatedApplyAmount(ShopOrder order, BigDecimal requestedAmount) {
        BigDecimal maxRefundAmount = resolveOrderPayPrice(order);
        BigDecimal applyAmount = requestedAmount == null ? maxRefundAmount : requestedAmount;
        CommonResponseEnum.INVALID.message("退款金额必须大于0")
                .assertTrue(applyAmount == null || applyAmount.compareTo(BigDecimal.ZERO) <= 0);
        CommonResponseEnum.INVALID.message("退款金额不能超过订单实付金额")
                .assertTrue(maxRefundAmount != null && applyAmount.compareTo(maxRefundAmount) > 0);
        return applyAmount;
    }

    private ShopOrderAfterSaleDTO.FrontList normalizeFrontQuery(ShopOrderAfterSaleDTO.FrontList dto) {
        ShopOrderAfterSaleDTO.FrontList query = dto == null ? new ShopOrderAfterSaleDTO.FrontList() : dto;
        if (query.getKeyword() != null) {
            query.setKeyword(query.getKeyword().trim());
        }
        return query;
    }

    private ShopOrderAfterSaleDTO.ListQuery normalizeAdminQuery(ShopOrderAfterSaleDTO.ListQuery dto) {
        ShopOrderAfterSaleDTO.ListQuery query = dto == null ? new ShopOrderAfterSaleDTO.ListQuery() : dto;
        if (query.getServiceNo() != null) {
            query.setServiceNo(query.getServiceNo().trim());
        }
        if (query.getOrderNo() != null) {
            query.setOrderNo(query.getOrderNo().trim());
        }
        return query;
    }

    private ShopOrderAfterSale buildAfterSaleEntity(Long userId,
                                                  ShopOrder order,
                                                  ShopOrderAfterSaleDTO.FrontApply dto,
                                                  String reason,
                                                  BigDecimal applyAmount) {
        ShopOrderAfterSale afterSale = new ShopOrderAfterSale();
        afterSale.setServiceNo(SerialNumberGenerator.generateBizOrderNo("AS"));
        afterSale.setOrderId(order.getId());
        afterSale.setOrderNo(order.getOrderId());
        afterSale.setUid(userId);
        afterSale.setAfterSaleType(dto.getAfterSaleType());
        afterSale.setStatus(ShopOrderAfterSaleConstant.STATUS_WAIT_AUDIT);
        afterSale.setApplyAmount(applyAmount);
        afterSale.setReason(reason);
        afterSale.setUserRemark(dto.getUserRemark() == null ? null : dto.getUserRemark().trim());
        afterSale.setIsDel(0);
        afterSale.setVersion(0);
        return afterSale;
    }

    private ShopOrderAfterSale buildReturnUpdateEntity(Long id, String expressName, String expressNo, String returnRemark) {
        ShopOrderAfterSale update = new ShopOrderAfterSale();
        update.setId(id);
        update.setStatus(ShopOrderAfterSaleConstant.STATUS_WAIT_MERCHANT_RECEIVE);
        update.setReturnExpressName(expressName);
        update.setReturnExpressNo(expressNo);
        update.setReturnRemark(returnRemark == null ? null : returnRemark.trim());
        update.setReturnTime(LocalDateTime.now());
        return update;
    }

    private ShopOrderAfterSale buildAuditUpdateEntity(Long id, Integer status, String auditRemark) {
        ShopOrderAfterSale update = new ShopOrderAfterSale();
        update.setId(id);
        update.setStatus(status);
        update.setAuditRemark(auditRemark);
        update.setAuditTime(LocalDateTime.now());
        return update;
    }

    private ShopOrderAfterSale buildRefundSuccessUpdateEntity(ShopOrderAfterSale afterSale,
                                                            String auditRemark,
                                                            String receiveRemark,
                                                            LocalDateTime now) {
        ShopOrderAfterSale update = new ShopOrderAfterSale();
        update.setId(afterSale.getId());
        update.setStatus(ShopOrderAfterSaleConstant.STATUS_REFUND_SUCCESS);
        update.setAuditRemark(auditRemark);
        update.setReceiveRemark(receiveRemark);
        if (afterSale.getAuditTime() == null) {
            update.setAuditTime(now);
        }
        if (afterSale.getAfterSaleType() != null && afterSale.getAfterSaleType() == ShopOrderAfterSaleConstant.TYPE_RETURN_REFUND) {
            update.setReceiveTime(now);
        }
        update.setRefundTime(now);
        return update;
    }

    private ShopUserBill buildRefundBill(Long uid, String serviceNo, BigDecimal refundAmount, BigDecimal newMoney) {
        ShopUserBill userBill = new ShopUserBill();
        userBill.setUid(uid);
        userBill.setLinkId(serviceNo);
        userBill.setPm(ShopPaymentConstant.PM_REFUND);
        userBill.setTitle("订单售后退款");
        userBill.setCategory(ShopPaymentConstant.CATEGORY_NOW_MONEY);
        userBill.setType(ShopPaymentConstant.TYPE_PAY_PRODUCT_REFUND);
        userBill.setNumber(refundAmount);
        userBill.setBalance(newMoney);
        userBill.setMark("售后退款" + refundAmount.toPlainString() + "元到余额");
        userBill.setStatus(1);
        return userBill;
    }

    private ShopUserIntegralBill buildRefundIntegralBill(Long uid, String serviceNo, int useIntegral, int newIntegral) {
        ShopUserIntegralBill integralBill = new ShopUserIntegralBill();
        integralBill.setUid(uid);
        integralBill.setLinkId(serviceNo);
        integralBill.setPm(ShopPaymentConstant.PM_REFUND);
        integralBill.setTitle("订单售后退回积分");
        integralBill.setCategory(ShopPaymentConstant.CATEGORY_INTEGRAL);
        integralBill.setType(ShopPaymentConstant.TYPE_USE_INTEGRAL_ORDER);
        integralBill.setNumber(useIntegral);
        integralBill.setBalance(newIntegral);
        integralBill.setMark("售后退回订单使用积分" + useIntegral);
        integralBill.setStatus(1);
        return integralBill;
    }

    private ShopFrontOrderAfterSaleVO assembleFrontDetail(ShopOrderAfterSale afterSale) {
        ShopFrontOrderAfterSaleVO vo = new ShopFrontOrderAfterSaleVO();
        copyToFront(afterSale, vo);
        vo.setOrderInfoList(assembleOrderProducts(loadOrder(afterSale.getOrderId())));
        return vo;
    }

    private ShopOrderAfterSaleVO assembleAdminDetail(ShopOrderAfterSale afterSale) {
        ShopOrder order = loadOrder(afterSale.getOrderId());
        ShopOrderAfterSaleVO vo = new ShopOrderAfterSaleVO();
        copyToAdmin(afterSale, order, vo);
        vo.setOrderInfoList(assembleOrderProducts(order));
        return vo;
    }

    private void enrichFrontSummaryWithProducts(ShopFrontOrderAfterSaleVO item) {
        fillFrontSummary(item);
        if (item.getOrderId() != null) {
            item.setOrderInfoList(assembleOrderProducts(loadOrder(item.getOrderId())));
        }
    }

    private void fillFrontSummary(ShopFrontOrderAfterSaleVO vo) {
        if (vo == null) {
            return;
        }
        vo.setAfterSaleTypeText(resolveAfterSaleTypeText(vo.getAfterSaleType()));
        vo.setStatusText(resolveAfterSaleStatusText(vo.getStatus()));
        vo.setCanFillReturnExpress(Boolean.TRUE.equals(canFillReturnExpress(vo.getAfterSaleType(), vo.getStatus())));
    }

    private void fillAdminSummary(ShopOrderAfterSaleVO vo) {
        if (vo == null) {
            return;
        }
        vo.setAfterSaleTypeText(resolveAfterSaleTypeText(vo.getAfterSaleType()));
        vo.setStatusText(resolveAfterSaleStatusText(vo.getStatus()));
    }

    private void copyToFront(ShopOrderAfterSale afterSale, ShopFrontOrderAfterSaleVO vo) {
        vo.setId(afterSale.getId());
        vo.setServiceNo(afterSale.getServiceNo());
        vo.setOrderId(afterSale.getOrderId());
        vo.setOrderNo(afterSale.getOrderNo());
        vo.setAfterSaleType(afterSale.getAfterSaleType());
        vo.setAfterSaleTypeText(resolveAfterSaleTypeText(afterSale.getAfterSaleType()));
        vo.setStatus(afterSale.getStatus());
        vo.setStatusText(resolveAfterSaleStatusText(afterSale.getStatus()));
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
        vo.setCanFillReturnExpress(canFillReturnExpress(afterSale.getAfterSaleType(), afterSale.getStatus()));
    }

    private void copyToAdmin(ShopOrderAfterSale afterSale, ShopOrder order, ShopOrderAfterSaleVO vo) {
        vo.setId(afterSale.getId());
        vo.setServiceNo(afterSale.getServiceNo());
        vo.setOrderId(afterSale.getOrderId());
        vo.setOrderNo(afterSale.getOrderNo());
        vo.setUid(afterSale.getUid());
        vo.setRealName(order.getRealName());
        vo.setUserPhone(order.getUserPhone());
        vo.setUserAddress(order.getUserAddress());
        vo.setAfterSaleType(afterSale.getAfterSaleType());
        vo.setAfterSaleTypeText(resolveAfterSaleTypeText(afterSale.getAfterSaleType()));
        vo.setStatus(afterSale.getStatus());
        vo.setStatusText(resolveAfterSaleStatusText(afterSale.getStatus()));
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
    }

    private List<ShopFrontOrderProductVO> assembleOrderProducts(ShopOrder order) {
        if (order == null || order.getCartIds() == null || order.getCartIds().isBlank()) {
            return Collections.emptyList();
        }
        List<Long> cartIds = parseOrderCartIdsQuietly(order.getCartIds());
        if (cartIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ShopProductCartAndSkuVO> cartRows = shopOrderMapper.selectOrderCartWithSku(cartIds, null, null);
        Map<Long, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < cartIds.size(); i++) {
            orderMap.put(cartIds.get(i), i);
        }
        cartRows.sort(Comparator.comparingInt(row -> orderMap.getOrDefault(row.getId(), Integer.MAX_VALUE)));

        return cartRows.stream().map(item -> {
            ShopFrontOrderProductVO vo = new ShopFrontOrderProductVO();
            vo.setProductId(item.getProductId());
            vo.setSkuId(item.getSkuId());
            vo.setProductName(item.getProductName());
            vo.setImage(item.getImage());
            vo.setSku(item.getSku());
            vo.setPrice(item.getPrice() == null ? "0" : item.getPrice().toPlainString());
            vo.setCartNum(item.getCartNum());
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 数据读取与持久化写入层 ====================

    private ShopOrder requireFrontOrder(String orderNo, Long userId) {
        ShopOrder order = shopOrderMapper.selectActiveByOrderNoAndUid(orderNo, userId);
        CommonResponseEnum.INVALID.message("订单不存在").assertNull(order);
        return order;
    }

    private ShopOrderAfterSale requireFrontAfterSale(Long id, Long userId) {
        ShopOrderAfterSale afterSale = shopOrderAfterSaleMapper.selectActiveByIdAndUid(id, userId);
        CommonResponseEnum.INVALID.message("售后单不存在").assertNull(afterSale);
        return afterSale;
    }

    private ShopOrderAfterSale requireAfterSale(Long id) {
        ShopOrderAfterSale afterSale = shopOrderAfterSaleMapper.selectActiveById(id);
        CommonResponseEnum.INVALID.message("售后单不存在").assertNull(afterSale);
        return afterSale;
    }

    private ShopOrder loadOrder(Long orderId) {
        CommonResponseEnum.INVALID_ID.assertNull(orderId);
        ShopOrder order = shopOrderMapper.selectOneById(orderId);
        CommonResponseEnum.INVALID.message("订单不存在").assertNull(order);
        return order;
    }

    private void persistAfterSaleApply(ShopOrderAfterSale afterSale, ShopOrder order, BigDecimal applyAmount, String reason) {
        save(afterSale);
        updateOrderRefundState(order, 1, applyAmount, reason, null);
        saveOrderStatusLog(order.getId(), ShopOrderStatusConstant.CHANGE_TYPE_AFTER_SALE_APPLY,
                "用户申请" + resolveAfterSaleTypeText(afterSale.getAfterSaleType()) + "，金额：" + applyAmount.toPlainString() + "元");
    }

    private void persistReturnSubmission(ShopOrderAfterSale afterSale,
                                         ShopOrderAfterSale updateEntity,
                                         String expressName,
                                         String expressNo) {
        boolean updated = updateById(updateEntity);
        CommonResponseEnum.INVALID.message("填写退货物流失败，请稍后重试").assertTrue(!updated);

        updateOrderRefundState(loadOrder(afterSale.getOrderId()), 1, afterSale.getApplyAmount(), null, null);
        saveOrderStatusLog(afterSale.getOrderId(), ShopOrderStatusConstant.CHANGE_TYPE_AFTER_SALE_RETURN,
                "用户已填写退货物流：" + expressName + " / " + expressNo);
    }

    private void persistAuditApprovedForReturn(ShopOrder order, ShopOrderAfterSale afterSale, ShopOrderAfterSale updateEntity) {
        boolean updated = updateById(updateEntity);
        CommonResponseEnum.INVALID.message("审核通过失败，请稍后重试").assertTrue(!updated);

        updateOrderRefundState(order, 1, afterSale.getApplyAmount(), null, null);
        saveOrderStatusLog(order.getId(), ShopOrderStatusConstant.CHANGE_TYPE_AFTER_SALE_APPROVE,
                "商家已同意退货退款，等待买家寄回商品");
    }

    private void persistAuditRejected(ShopOrder order, ShopOrderAfterSale updateEntity, String auditRemark) {
        boolean updated = updateById(updateEntity);
        CommonResponseEnum.INVALID.message("审核驳回失败，请稍后重试").assertTrue(!updated);

        updateOrderRefundState(order, 3, order.getRefundPrice(), order.getRefundReasonExplain(), auditRemark);
        saveOrderStatusLog(order.getId(), ShopOrderStatusConstant.CHANGE_TYPE_AFTER_SALE_REJECT,
                "商家已拒绝售后申请" + (auditRemark == null || auditRemark.isBlank() ? "" : "，原因：" + auditRemark));
    }

    private void completeRefund(ShopOrderAfterSale afterSale, ShopOrder order, String auditRemark, String receiveRemark) {
        BigDecimal refundAmount = afterSale.getApplyAmount() == null ? BigDecimal.ZERO : afterSale.getApplyAmount();
        persistRefundAssets(order, afterSale, refundAmount);

        ShopOrderAfterSale updateEntity = buildRefundSuccessUpdateEntity(afterSale, auditRemark, receiveRemark, LocalDateTime.now());
        boolean updated = updateById(updateEntity);
        CommonResponseEnum.INVALID.message("更新售后退款状态失败，请稍后重试").assertTrue(!updated);

        updateOrderRefundState(order, 2, refundAmount, order.getRefundReasonExplain(), null);
        saveOrderStatusLog(order.getId(), ShopOrderStatusConstant.CHANGE_TYPE_AFTER_SALE_REFUND,
                "售后退款已完成，退款金额：" + refundAmount.toPlainString() + "元");
    }

    private void persistRefundAssets(ShopOrder order, ShopOrderAfterSale afterSale, BigDecimal refundAmount) {
        ShopUser user = shopUserService.getById(order.getUid());
        CommonResponseEnum.INVALID.message("用户不存在").assertNull(user);

        BigDecimal nowMoney = user.getNowMoney() == null ? BigDecimal.ZERO : user.getNowMoney();
        BigDecimal newMoney = nowMoney.add(refundAmount);
        int useIntegral = order.getUseIntegral() == null ? 0 : Math.max(order.getUseIntegral(), 0);
        int currentIntegral = user.getIntegral() == null ? 0 : user.getIntegral();
        int newIntegral = currentIntegral + useIntegral;

        ShopUser updateUser = new ShopUser();
        updateUser.setUid(user.getUid());
        updateUser.setNowMoney(newMoney);
        if (useIntegral > 0) {
            updateUser.setIntegral(newIntegral);
        }
        boolean updated = shopUserService.updateById(updateUser);
        CommonResponseEnum.INVALID.message("退回用户资产失败，请稍后重试").assertTrue(!updated);

        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            ShopUserBill userBill = buildRefundBill(user.getUid(), afterSale.getServiceNo(), refundAmount, newMoney);
            boolean saved = shopUserBillService.save(userBill);
            CommonResponseEnum.INVALID.message("写入退款账单失败，请稍后重试").assertTrue(!saved);
        }

        if (useIntegral > 0) {
            ShopUserIntegralBill integralBill = buildRefundIntegralBill(user.getUid(), afterSale.getServiceNo(), useIntegral, newIntegral);
            boolean saved = shopUserIntegralBillService.save(integralBill);
            CommonResponseEnum.INVALID.message("写入积分退回流水失败，请稍后重试").assertTrue(!saved);
        }
    }

    private void updateOrderRefundState(ShopOrder order, Integer refundStatus, BigDecimal refundPrice, String refundReasonExplain, String refundReason) {
        if (order == null || order.getId() == null) {
            return;
        }
        ShopOrder update = new ShopOrder();
        update.setId(order.getId());
        update.setRefundStatus(refundStatus);
        if (refundPrice != null) {
            update.setRefundPrice(refundPrice);
        }
        if (refundReasonExplain != null) {
            update.setRefundReasonExplain(refundReasonExplain);
        }
        if (refundReason != null) {
            update.setRefundReason(refundReason);
        }
        shopOrderMapper.update(update);
    }

    private void saveOrderStatusLog(Long orderId, String changeType, String changeMessage) {
        if (orderId == null || changeType == null || changeType.isBlank() || changeMessage == null || changeMessage.isBlank()) {
            return;
        }
        ShopOrderStatus orderStatus = new ShopOrderStatus();
        orderStatus.setOid(orderId);
        orderStatus.setChangeType(changeType);
        orderStatus.setChangeMessage(changeMessage);
        shopOrderStatusService.save(orderStatus);
    }

    // ==================== 辅助工具方法 ====================

    private Boolean canFillReturnExpress(Integer afterSaleType, Integer status) {
        return afterSaleType != null
                && afterSaleType == ShopOrderAfterSaleConstant.TYPE_RETURN_REFUND
                && status != null
                && status == ShopOrderAfterSaleConstant.STATUS_WAIT_BUYER_RETURN;
    }

    private BigDecimal resolveOrderPayPrice(ShopOrder order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }
        if (order.getPayPrice() != null) {
            return order.getPayPrice();
        }
        BigDecimal totalPrice = order.getTotalPrice() == null ? BigDecimal.ZERO : order.getTotalPrice();
        BigDecimal freightPrice = order.getFreightPrice() == null ? BigDecimal.ZERO : order.getFreightPrice();
        BigDecimal discount = order.getDeductionPrice();
        if (discount == null) {
            discount = order.getCouponPrice();
        }
        if (discount == null) {
            discount = BigDecimal.ZERO;
        }
        return totalPrice.add(freightPrice).subtract(discount).max(BigDecimal.ZERO);
    }

    private String resolveAfterSaleTypeText(Integer type) {
        return ShopOrderAfterSaleTypeEnum.getDesc(type);
    }

    private String resolveAfterSaleStatusText(Integer status) {
        return ShopOrderAfterSaleStatusEnum.getDesc(status);
    }

    private static List<Long> parseOrderCartIdsQuietly(String cartIdsRaw) {
        if (cartIdsRaw == null || cartIdsRaw.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(cartIdsRaw.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> s.matches("\\d+"))
                .map(Long::valueOf)
                .distinct()
                .collect(Collectors.toList());
    }
}
