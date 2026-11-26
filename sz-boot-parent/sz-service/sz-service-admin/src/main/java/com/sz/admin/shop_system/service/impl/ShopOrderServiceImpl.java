package com.sz.admin.shop_system.service.impl;

import cn.hutool.json.JSONUtil;
import com.github.pagehelper.PageHelper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sz.admin.shop_system.agent.pojo.dto.AgentMallDTO;
import com.sz.admin.shop_system.agent.pojo.vo.AgentMallVO;
import com.sz.admin.shop_system.constants.ShopOrderStatusConstant;
import com.sz.admin.shop_system.constants.ShopPaymentConstant;
import com.sz.admin.shop_system.enums.ShopOrderStatusEnum;
import com.sz.admin.shop_system.mapper.ShopExpressInfoMapper;
import com.sz.admin.shop_system.mapper.ShopExpressTemplateMapper;
import com.sz.admin.shop_system.mapper.ShopOrderAfterSaleMapper;
import com.sz.admin.shop_system.mapper.ShopOrderMapper;
import com.sz.admin.shop_system.mapper.ShopProductAttrValueMapper;
import com.sz.admin.shop_system.mapper.ShopProductCartMapper;
import com.sz.admin.shop_system.mapper.ShopProductCommentMapper;
import com.sz.admin.shop_system.mapper.ShopProductCouponMapper;
import com.sz.admin.shop_system.mapper.ShopProductCouponUserMapper;
import com.sz.admin.shop_system.mapper.ShopProductMapper;
import com.sz.admin.shop_system.mapper.ShopUserAddressMapper;
import com.sz.admin.shop_system.pojo.dto.ShopFlashSaleDTO;
import com.sz.admin.shop_system.pojo.dto.ShopOrderDTO;
import com.sz.admin.shop_system.pojo.dto.ShopOrderStatusDTO;
import com.sz.admin.shop_system.pojo.po.ShopExpress;
import com.sz.admin.shop_system.pojo.po.ShopExpressInfo;
import com.sz.admin.shop_system.pojo.po.ShopExpressTemplate;
import com.sz.admin.shop_system.pojo.po.ShopFlashSale;
import com.sz.admin.shop_system.pojo.po.ShopOrder;
import com.sz.admin.shop_system.pojo.po.ShopOrderAfterSale;
import com.sz.admin.shop_system.pojo.po.ShopOrderStatus;
import com.sz.admin.shop_system.pojo.po.ShopProduct;
import com.sz.admin.shop_system.pojo.po.ShopProductAttrValue;
import com.sz.admin.shop_system.pojo.po.ShopProductCart;
import com.sz.admin.shop_system.pojo.po.ShopProductComment;
import com.sz.admin.shop_system.pojo.po.ShopProductCoupon;
import com.sz.admin.shop_system.pojo.po.ShopProductCouponUser;
import com.sz.admin.shop_system.pojo.po.ShopUser;
import com.sz.admin.shop_system.pojo.po.ShopUserAddress;
import com.sz.admin.shop_system.pojo.po.ShopUserBill;
import com.sz.admin.shop_system.pojo.po.ShopUserIntegralBill;
import com.sz.admin.shop_system.pojo.vo.ShopCouponAndUserVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderDetailVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderProductVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderVO;
import com.sz.admin.shop_system.pojo.vo.ShopFrontProductCommentVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderAdminDetailVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderCreateVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderPaymentInfoVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderStatusVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderVO;
import com.sz.admin.shop_system.pojo.vo.ShopPaymentVO;
import com.sz.admin.shop_system.pojo.vo.ShopPreOrderVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductCartAndSkuVO;
import com.sz.admin.shop_system.properties.ShopPaymentProperties;
import com.sz.admin.shop_system.service.ShopExpressService;
import com.sz.admin.shop_system.service.ShopFlashSaleService;
import com.sz.admin.shop_system.service.ShopOrderService;
import com.sz.admin.shop_system.service.ShopOrderStatusService;
import com.sz.admin.shop_system.service.ShopProductAttrValueService;
import com.sz.admin.shop_system.service.ShopProductService;
import com.sz.admin.shop_system.service.ShopUserBillService;
import com.sz.admin.shop_system.service.ShopUserIntegralBillService;
import com.sz.admin.shop_system.service.ShopUserService;
import com.sz.admin.shop_system.util.SerialNumberGenerator;
import com.sz.core.common.entity.PageResult;
import com.sz.core.common.entity.SelectIdsDTO;
import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.core.util.BeanCopyUtils;
import com.sz.core.util.PageUtils;
import com.sz.security.util.StpUserUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * <p>
 * 架构分层：业务编排 -> 数据计算与组装（assemble/build） -> 数据库持久化写入（persist/deduct/write）。
 *
 * @author car
 * @since 2026-01-28
 */
@Service
@RequiredArgsConstructor
public class ShopOrderServiceImpl extends ServiceImpl<ShopOrderMapper, ShopOrder> implements ShopOrderService {

    private static final Set<String> SUPPORTED_PAY_TYPES = new HashSet<>(Arrays.asList("yue", "wxpay", "alipay"));
    private static final int ORDER_PAID_PENDING = 0;
    private static final long ORDER_INTEGRAL_DEDUCTION_RATIO = 10000L;
    private static final int ORDER_PAID_SUCCESS = 1;
    private static final int ORDER_PAID_FAILED = 2;
    private static final String ORDER_PAYMENT_TTL_KEY_PREFIX = "shop:order:payment:ttl:";
    private static final String FLASH_RESERVE_KEY_PREFIX = "shop:flash:reserve:";
    private static final String AGENT_ORDER_IDEMP_KEY_PREFIX = "shop:agent:order:idemp:";
    private static final long AGENT_ORDER_IDEMP_TTL_HOURS = 24L;
    private static final long AGENT_ORDER_LOCK_TTL_SECONDS = 30L;
    private static final int ORDER_TYPE_NORMAL = 0;
    private static final int ORDER_TYPE_FLASH = 1;

    private final ShopUserService shopUserService;
    private final ShopProductCartMapper shopProductCartMapper;
    private final ShopOrderStatusService shopOrderStatusService;
    private final ShopProductAttrValueService shopProductAttrValueService;
    private final ShopProductService shopProductService;
    private final ShopProductMapper shopProductMapper;
    private final ShopProductAttrValueMapper shopProductAttrValueMapper;
    private final ShopUserBillService shopUserBillService;
    private final ShopUserIntegralBillService shopUserIntegralBillService;
    private final ShopExpressService shopExpressService;
    private final ShopExpressInfoMapper shopExpressInfoMapper;
    private final ShopExpressTemplateMapper shopExpressTemplateMapper;
    private final ShopUserAddressMapper shopUserAddressMapper;
    private final ShopProductCouponMapper shopProductCouponMapper;
    private final ShopProductCouponUserMapper shopProductCouponUserMapper;
    private final ShopProductCommentMapper shopProductCommentMapper;
    private final ShopOrderAfterSaleMapper shopOrderAfterSaleMapper;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final ShopPaymentProperties shopPaymentProperties;
    private final ShopFlashSaleService shopFlashSaleService;

    // ==================== 一、业务编排层 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopPreOrderVO create(ShopOrderDTO.PreOrder dto) {
        long userId = StpUserUtil.getLoginIdAsLong();
        ShopUser shopUser = shopUserService.getById(userId);
        List<Integer> distinctCartIds = validateAndExtractCartIds(dto == null ? null : dto.getShoppingCartIds());

        List<ShopProductCartAndSkuVO> cartRows = loadAndVerifyActiveCarts(userId, distinctCartIds);
        CartSummary summary = summarizeCartItems(cartRows);

        String orderNo = SerialNumberGenerator.generateOrderNo();
        ShopOrder order = buildNormalPreOrderEntity(orderNo, userId, shopUser, distinctCartIds, summary);
        persistNormalPreOrder(order, userId, distinctCartIds);

        return assembleCreatedPreOrderVO(orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopOrderCreateVO reserveFlashSale(ShopFlashSaleDTO.Reserve dto) {
        CommonResponseEnum.INVALID.message("秒杀参数不能为空").assertNull(dto);
        CommonResponseEnum.INVALID_ID.assertNull(dto.getProductId());
        CommonResponseEnum.INVALID_ID.assertNull(dto.getSkuId());

        long userId = StpUserUtil.getLoginIdAsLong();
        int cartNum = dto.getCartNum() == null || dto.getCartNum() <= 0 ? 1 : dto.getCartNum();
        LocalDateTime now = LocalDateTime.now();

        ShopFlashSale flashSale = shopFlashSaleService.getAvailableByProductId(dto.getProductId(), now);
        CommonResponseEnum.INVALID.message("秒杀活动不存在或未开始").assertNull(flashSale);

        int limitPerUser = flashSale.getLimitPerUser() == null || flashSale.getLimitPerUser() <= 0 ? 1 : flashSale.getLimitPerUser();
        CommonResponseEnum.INVALID.message("超过限购数量").assertTrue(cartNum > limitPerUser);

        ShopProduct product = shopProductService.getById(dto.getProductId());
        CommonResponseEnum.INVALID.message("商品不存在").assertNull(product);
        CommonResponseEnum.INVALID.message("商品未上架").assertTrue(product.getIsShow() == null || product.getIsShow() != 1);

        ShopProductAttrValue sku = shopProductAttrValueService.getById(dto.getSkuId());
        CommonResponseEnum.INVALID.message("商品规格不存在").assertNull(sku);
        CommonResponseEnum.INVALID.message("商品规格与商品不匹配")
                .assertTrue(sku.getProductId() == null || !dto.getProductId().equals(sku.getProductId()));

        String lockKey = buildFlashReserveLockKey(flashSale.getId(), sku.getId());
        boolean lockSuccess = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, String.valueOf(userId), 5, TimeUnit.SECONDS));
        CommonResponseEnum.INVALID.message("当前抢购人数较多，请稍后重试").assertTrue(!lockSuccess);
        try {
            return executeLockedFlashSaleReserve(userId, dto, cartNum, flashSale);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public void update(ShopOrderDTO.Update dto) {
        ShopOrder shopOrder = getById(dto.getId());
        CommonResponseEnum.INVALID_ID.assertNull(shopOrder);

        Integer newStatus = dto.getStatus();
        Integer currentStatus = shopOrder.getStatus() == null ? ShopOrderStatusEnum.CREATED.getCode() : shopOrder.getStatus();

        if (newStatus != null && !newStatus.equals(currentStatus)) {
            CommonResponseEnum.INVALID.message("订单状态无效").assertNull(ShopOrderStatusEnum.fromCode(newStatus));
            CommonResponseEnum.INVALID.message("订单状态流转无效")
                    .assertTrue(!ShopOrderStatusEnum.canTransfer(currentStatus, newStatus));
            shopOrder.setStatus(newStatus);
            saveOrUpdate(shopOrder);
            addOrderStatusLog(shopOrder, newStatus);
        }
    }

    @Override
    public void remove(SelectIdsDTO dto) {
        CommonResponseEnum.INVALID_ID.assertTrue(dto.getIds().isEmpty());
        removeByIds(dto.getIds());
    }

    @Override
    public ShopPreOrderVO queryDetail(String orderNo) {
        long userId = StpUserUtil.getLoginIdAsLong();

        ShopOrder shopOrder = requireFrontOrder(orderNo, userId);
        long flashRemainingSeconds = refreshFlashReserveState(shopOrder);
        assertOrderPending(shopOrder);

        ShopUserAddress defaultAddress = shopUserAddressMapper.selectDefaultByUid(userId);
        Long defaultAddressId = defaultAddress == null ? null : defaultAddress.getId();

        List<Long> distinctCartIds = parseOrderCartIds(shopOrder.getCartIds());
        List<ShopProductCartAndSkuVO> cartRows = loadAndSortPreOrderCartRows(userId, distinctCartIds);
        CartSummary summary = summarizeCartItems(cartRows);

        long maxUseIntegral = resolveMaxUsableIntegral(userId, summary.getTotalPrice());
        List<Long> expiredUserCouponIds = new ArrayList<>();
        List<ShopCouponAndUserVO> couponVos = assembleAvailableCoupons(userId, summary, expiredUserCouponIds);
        persistExpiredUserCoupons(expiredUserCouponIds, LocalDateTime.now());

        return assemblePreOrderDetailVO(shopOrder, defaultAddressId, summary, cartRows, couponVos, maxUseIntegral, flashRemainingSeconds);
    }

    @Override
    public ShopPreOrderVO recalculate(ShopOrderDTO.Recalculate dto) {
        CommonResponseEnum.INVALID.message("预下单号不能为空")
                .assertTrue(dto == null || dto.getPreOrderNo() == null || dto.getPreOrderNo().isBlank());
        long userId = StpUserUtil.getLoginIdAsLong();

        ShopPreOrderVO vo = queryDetail(dto.getPreOrderNo());
        ShopOrder order = requireFrontOrder(dto.getPreOrderNo(), userId);
        long flashRemainingSeconds = refreshFlashReserveState(order);
        assertOrderPending(order);
        if (resolveOrderType(order) == ORDER_TYPE_FLASH) {
            CommonResponseEnum.INVALID.message("秒杀订单已超时，请重新抢购").assertTrue(flashRemainingSeconds <= 0);
        }

        ShopUserAddress selectedAddress = resolveOptionalUserAddress(userId, dto.getAddressId());
        Long selectedAddressId = selectedAddress != null ? selectedAddress.getId() : vo.getAddressId();

        BigDecimal goodsTotal = new BigDecimal(vo.getPayFee());
        BigDecimal freightTotal = new BigDecimal(vo.getFreightTotalFee());

        CouponChoice couponChoice = resolveCouponForRecalculate(vo, dto, goodsTotal);
        IntegralDeduction integralDeduction = calculateIntegralDeduction(userId, Boolean.TRUE.equals(dto.getIsUseIntegral()),
                goodsTotal.subtract(couponChoice.getCouponPrice()));

        BigDecimal payAfterDiscount = goodsTotal.subtract(couponChoice.getCouponPrice())
                .subtract(integralDeduction.getDeductionPrice()).max(BigDecimal.ZERO);
        BigDecimal finalTotal = payAfterDiscount.add(freightTotal);

        applyRecalculatedFieldsToVO(vo, selectedAddressId, payAfterDiscount, finalTotal,
                Boolean.TRUE.equals(dto.getIsUseIntegral()), couponChoice, integralDeduction);

        ShopOrder updateEntity = buildRecalculateOrderUpdate(order.getId(), selectedAddress, couponChoice, integralDeduction, finalTotal);
        updateById(updateEntity);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopOrderCreateVO orderCreate(ShopOrderDTO.Create dto) {
        CommonResponseEnum.INVALID.message("创建订单参数不能为空").assertNull(dto);
        CommonResponseEnum.INVALID.message("预下单号不能为空")
                .assertTrue(dto.getPreOrderNo() == null || dto.getPreOrderNo().isBlank());
        CommonResponseEnum.INVALID.message("收货地址不能为空")
                .assertTrue(dto.getAddressId() == null || dto.getAddressId().isBlank());
        Long addressId = parseAddressId(dto.getAddressId());

        long userId = StpUserUtil.getLoginIdAsLong();
        ShopOrder order = requireFrontOrder(dto.getPreOrderNo(), userId);
        assertOrderPending(order);

        ShopUserAddress address = requireUserAddress(userId, addressId);
        ShopPreOrderVO detail = queryDetail(dto.getPreOrderNo());
        BigDecimal goodsTotal = new BigDecimal(detail.getPayFee());
        BigDecimal freightTotal = new BigDecimal(detail.getFreightTotalFee());
        CartSummary summary = summarizeCartItems(detail.getOrderInfoList());

        CouponConfirmResult couponResult = resolveAndValidateCouponForCreate(userId, dto.getPlatUserCouponId(), goodsTotal, summary);
        IntegralDeduction integralDeduction = calculateIntegralDeduction(userId, Boolean.TRUE.equals(dto.getIsUseIntegral()),
                goodsTotal.subtract(couponResult.getCouponDiscount()));

        BigDecimal payPrice = goodsTotal.add(freightTotal)
                .subtract(couponResult.getCouponDiscount())
                .subtract(integralDeduction.getDeductionPrice())
                .max(BigDecimal.ZERO);

        ShopOrder updateEntity = buildOrderConfirmUpdate(order.getId(), address, couponResult, integralDeduction, payPrice);
        persistOrderConfirmAndCoupon(order, updateEntity, userId, couponResult.getUsedCouponUserId());

        initializeOrderPaymentCountdown(order);
        if (resolveOrderType(order) == ORDER_TYPE_FLASH) {
            consumeFlashReserve(order.getOrderId());
            saveOrderStatusLog(order.getId(), ShopOrderStatusConstant.CHANGE_TYPE_FLASH_SUBMIT, ShopOrderStatusConstant.CHANGE_MESSAGE_FLASH_SUBMIT);
        }

        return assembleOrderCreateVO(order.getOrderId(), resolveOrderType(order), payPrice);
    }

    @Override
    public ShopOrderPaymentInfoVO paymentInfo(String orderNo) {
        CommonResponseEnum.INVALID.message("订单号不能为空")
                .assertTrue(orderNo == null || orderNo.isBlank());

        long userId = StpUserUtil.getLoginIdAsLong();
        ShopOrder order = requireFrontOrder(orderNo, userId);
        long remainingSeconds = ensureOrderPaymentRemainingSeconds(order);
        ShopUser shopUser = shopUserService.getById(userId);

        return assemblePaymentInfoVO(order, shopUser, remainingSeconds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopPaymentVO payment(ShopOrderDTO.Payment dto) {
        CommonResponseEnum.INVALID.message("支付参数不能为空").assertNull(dto);
        CommonResponseEnum.INVALID.message("订单号不能为空")
                .assertTrue(dto.getOrderNo() == null || dto.getOrderNo().isBlank());

        String payType = resolvePayType(dto);
        CommonResponseEnum.INVALID.message("支付类型无效，支持：yue/wxpay/alipay")
                .assertTrue(!SUPPORTED_PAY_TYPES.contains(payType));

        long userId = StpUserUtil.getLoginIdAsLong();
        ShopOrder order = requireFrontOrder(dto.getOrderNo(), userId);
        assertOrderPending(order);
        long remainingSeconds = ensureOrderPaymentRemainingSeconds(order);
        CommonResponseEnum.INVALID.message("订单支付超时，已自动取消").assertTrue(remainingSeconds <= 0);

        BigDecimal payPrice = resolvePayPrice(order);
        CommonResponseEnum.INVALID.message("订单支付金额无效").assertTrue(payPrice.compareTo(BigDecimal.ZERO) < 0);

        ShopUser shopUser = shopUserService.getById(userId);
        CommonResponseEnum.INVALID.message("用户不存在").assertNull(shopUser);
        BigDecimal userMoney = shopUser.getNowMoney() == null ? BigDecimal.ZERO : shopUser.getNowMoney();
        CommonResponseEnum.INVALID.message("余额不足").assertTrue(userMoney.compareTo(payPrice) < 0);

        int useIntegral = order.getUseIntegral() == null ? 0 : Math.max(order.getUseIntegral(), 0);
        int currentIntegral = shopUser.getIntegral() == null ? 0 : shopUser.getIntegral();
        CommonResponseEnum.INVALID.message("积分不足，请刷新订单后重试").assertTrue(currentIntegral < useIntegral);

        BigDecimal remainMoney = userMoney.subtract(payPrice);
        int remainIntegral = currentIntegral - useIntegral;
        persistPaymentSettlement(order, userId, payType, payPrice, remainMoney, useIntegral, remainIntegral);

        return assemblePaymentVO(order.getOrderId(), payType);
    }

    @Override
    public PageResult<ShopOrderVO> page(ShopOrderDTO.ListQuery dto) {
        ShopOrderDTO.ListQuery query = dto == null ? new ShopOrderDTO.ListQuery() : dto;
        try {
            PageUtils.toPage(query);
            List<ShopOrderVO> rows = mapper.selectAdminOrderList(query);
            return PageUtils.getPageResult(rows);
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public PageResult<ShopFrontOrderVO> frontPage(ShopOrderDTO.FrontList dto) {
        return frontPageForUser(StpUserUtil.getLoginIdAsLong(), dto);
    }

    @Override
    public PageResult<ShopFrontOrderVO> frontPageForUser(Long userId, ShopOrderDTO.FrontList dto) {
        CommonResponseEnum.INVALID_TOKEN.assertTrue(userId == null || userId <= 0);
        ShopOrderDTO.FrontList query = dto == null ? new ShopOrderDTO.FrontList() : dto;

        List<ShopOrder> orderList = mapper.selectActiveListByUid(userId);
        List<ShopFrontOrderVO> rows = orderList.stream()
                .map(this::assembleFrontOrderSummary)
                .filter(Objects::nonNull)
                .filter(item -> matchesFrontOrderTab(item, query.getTab()))
                .filter(item -> matchesFrontOrderKeyword(item, query.getKeyword()))
                .collect(Collectors.toList());

        return sliceFrontOrderPage(rows, query.getPage(), query.getLimit());
    }

    @Override
    public ShopFrontOrderDetailVO frontDetail(String orderNo) {
        return frontDetailForUser(StpUserUtil.getLoginIdAsLong(), orderNo);
    }

    @Override
    public ShopFrontOrderDetailVO frontDetailForUser(Long userId, String orderNo) {
        CommonResponseEnum.INVALID_TOKEN.assertTrue(userId == null || userId <= 0);
        CommonResponseEnum.INVALID.message("订单号不能为空").assertTrue(orderNo == null || orderNo.isBlank());

        ShopOrder order = requireFrontOrder(orderNo, userId);
        refreshFrontOrderPaymentState(order);

        ShopFrontOrderVO summary = assembleFrontOrderSummary(order);
        List<ShopOrderStatusVO> orderStatusList = loadOrderStatusTimeline(order.getId(), true);
        return assembleFrontOrderDetailVO(order, summary, orderStatusList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentMallVO.DirectOrderResult directCreateForAgent(Long userId, AgentMallDTO.DirectCreateOrder dto) {
        validateDirectCreateRequest(userId, dto);

        String idempKey = buildAgentOrderIdempKey(userId, dto.getIdempotencyKey());
        AgentMallVO.DirectOrderResult cached = readAgentIdempotentOrder(idempKey);
        if (cached != null) {
            return cached;
        }

        String lockKey = idempKey + ":lock";
        boolean lockSuccess = Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(lockKey, String.valueOf(userId), AGENT_ORDER_LOCK_TTL_SECONDS, TimeUnit.SECONDS));
        CommonResponseEnum.INVALID.message("订单正在提交中，请稍后重试").assertTrue(!lockSuccess);
        try {
            AgentMallVO.DirectOrderResult doubleChecked = readAgentIdempotentOrder(idempKey);
            if (doubleChecked != null) {
                return doubleChecked;
            }
            AgentMallVO.DirectOrderResult created = executeDirectCreateForAgent(userId, dto);
            redisTemplate.opsForValue().set(idempKey, JSONUtil.toJsonStr(created), AGENT_ORDER_IDEMP_TTL_HOURS, TimeUnit.HOURS);
            return created;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public PageResult<ShopFrontProductCommentVO> frontCommentPage(ShopOrderDTO.FrontCommentList dto) {
        long userId = StpUserUtil.getLoginIdAsLong();
        ShopOrderDTO.FrontCommentList query = dto == null ? new ShopOrderDTO.FrontCommentList() : dto;
        String keyword = query.getKeyword() == null ? null : query.getKeyword().trim();

        try {
            PageUtils.toPage(query);
            List<ShopFrontProductCommentVO> rows = mapper.selectFrontCommentList(userId, keyword);
            return PageUtils.getPageResult(rows);
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void frontConfirmReceipt(String orderNo) {
        long userId = StpUserUtil.getLoginIdAsLong();
        ShopOrder order = requireFrontOrder(orderNo, userId);
        refreshFrontOrderPaymentState(order);

        CommonResponseEnum.INVALID.message("当前订单不允许确认收货")
                .assertTrue(normalizePaidStatus(order.getPaid()) != ORDER_PAID_SUCCESS || order.getStatus() == null || order.getStatus() != 1);

        int gainIntegral = resolveOrderGainIntegral(order);
        persistConfirmReceipt(order, userId, gainIntegral);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void frontEvaluate(ShopOrderDTO.FrontEvaluate dto) {
        CommonResponseEnum.INVALID.message("评价参数不能为空").assertNull(dto);
        CommonResponseEnum.INVALID.message("订单号不能为空")
                .assertTrue(dto.getOrderNo() == null || dto.getOrderNo().isBlank());

        long userId = StpUserUtil.getLoginIdAsLong();
        ShopOrder order = requireFrontOrder(dto.getOrderNo(), userId);
        refreshFrontOrderPaymentState(order);

        CommonResponseEnum.INVALID.message("当前订单暂不可评价")
                .assertTrue(normalizePaidStatus(order.getPaid()) != ORDER_PAID_SUCCESS || order.getStatus() == null || order.getStatus() != 2);

        String contentText = dto.getContent() == null ? "" : dto.getContent().trim();
        CommonResponseEnum.INVALID.message("请输入评价内容").assertTrue(contentText.isBlank());
        CommonResponseEnum.INVALID.message("评价内容不能超过200字").assertTrue(contentText.length() > 200);

        int score = dto.getScore() == null ? 5 : dto.getScore();
        CommonResponseEnum.INVALID.message("评分范围必须在1到5之间").assertTrue(score < 1 || score > 5);

        List<ShopProductComment> comments = buildProductComments(order, userId, score, contentText);
        persistOrderEvaluation(order, comments);
    }

    @Override
    public void frontRemove(String orderNo) {
        long userId = StpUserUtil.getLoginIdAsLong();
        ShopOrder order = requireFrontOrder(orderNo, userId);
        refreshFrontOrderPaymentState(order);

        String displayTab = resolveFrontDisplayTab(order);
        CommonResponseEnum.INVALID.message("当前订单不允许删除")
                .assertTrue(!"cancelled".equals(displayTab) && !"completed".equals(displayTab));

        ShopOrder update = new ShopOrder();
        update.setId(order.getId());
        update.setIsDel(1);
        boolean removed = updateById(update);
        CommonResponseEnum.INVALID.message("删除订单失败，请稍后重试").assertTrue(!removed);
    }

    @Override
    public ShopOrderVO detail(Object id) {
        ShopOrder shopOrder = getById((Serializable) id);
        CommonResponseEnum.INVALID_ID.assertNull(shopOrder);
        return BeanCopyUtils.copy(shopOrder, ShopOrderVO.class);
    }

    @Override
    public ShopOrderAdminDetailVO adminDetail(String orderNo) {
        CommonResponseEnum.INVALID.message("订单号不能为空")
                .assertTrue(orderNo == null || orderNo.isBlank());

        ShopOrder order = mapper.selectByOrderNo(orderNo);
        CommonResponseEnum.INVALID.message("订单不存在").assertNull(order);

        List<ShopProductCartAndSkuVO> cartRows = fetchOrderCartRows(parseOrderCartIdsQuietly(order.getCartIds()));
        List<ShopOrderStatusVO> orderStatusList = loadOrderStatusTimeline(order.getId(), false);
        return assembleAdminOrderDetailVO(order, cartRows, orderStatusList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliver(ShopOrderDTO.Deliver dto) {
        CommonResponseEnum.INVALID.message("发货参数不能为空").assertNull(dto);
        CommonResponseEnum.INVALID.message("订单号不能为空")
                .assertTrue(dto.getOrderNo() == null || dto.getOrderNo().isBlank());
        CommonResponseEnum.INVALID.message("快递模板不能为空")
                .assertTrue(dto.getExpressTemplateId() == null);

        ShopOrder order = mapper.selectByOrderNo(dto.getOrderNo());
        CommonResponseEnum.INVALID.message("订单不存在").assertNull(order);
        CommonResponseEnum.INVALID.message("订单未支付，不允许发货")
                .assertTrue(order.getPaid() == null || order.getPaid() != 1);
        CommonResponseEnum.INVALID.message("当前订单状态不允许发货")
                .assertTrue(order.getStatus() == null || order.getStatus() != 0);

        ShopExpressTemplate expressTemplate = shopExpressTemplateMapper.selectActiveById(dto.getExpressTemplateId());
        CommonResponseEnum.INVALID.message("快递模板不存在").assertNull(expressTemplate);
        CommonResponseEnum.INVALID.message("快递模板未绑定快递公司")
                .assertNull(expressTemplate.getExpressId());

        String expressNo = SerialNumberGenerator.generateExpressNo();
        ShopExpressInfo expressInfo = buildDeliverExpressInfo(order, expressTemplate, expressNo);
        ShopOrder orderUpdate = buildDeliverOrderUpdate(order.getId(), expressTemplate.getExpressId(), expressNo);
        persistDeliverAction(order.getId(), expressTemplate.getExpressId(), expressInfo, orderUpdate);
    }

    // ==================== 二、数据计算与组装层 ====================

    private ShopOrderCreateVO executeLockedFlashSaleReserve(long userId,
                                                          ShopFlashSaleDTO.Reserve dto,
                                                          int cartNum,
                                                          ShopFlashSale flashSale) {
        ShopProductAttrValue latestSku = shopProductAttrValueService.getById(dto.getSkuId());
        CommonResponseEnum.INVALID.message("商品规格不存在").assertNull(latestSku);
        int skuStock = latestSku.getStock() == null ? 0 : latestSku.getStock();
        CommonResponseEnum.INVALID.message("秒杀商品库存不足").assertTrue(skuStock < cartNum);

        ShopProduct latestProduct = shopProductService.getById(dto.getProductId());
        CommonResponseEnum.INVALID.message("商品不存在").assertNull(latestProduct);
        int aggregatedStock = resolveAggregatedProductStock(dto.getProductId(), latestProduct);
        CommonResponseEnum.INVALID.message("秒杀商品库存不足").assertTrue(aggregatedStock < cartNum);

        deductFlashStockOptimistically(latestSku, skuStock, latestProduct, aggregatedStock, cartNum);

        BigDecimal flashPrice = latestSku.getFlashPrice() != null
                ? latestSku.getFlashPrice()
                : (flashSale.getFlashPrice() == null ? latestSku.getPrice() : flashSale.getFlashPrice());
        BigDecimal totalPrice = flashPrice.multiply(BigDecimal.valueOf(cartNum));
        BigDecimal freightPrice = latestProduct.getPostage() == null ? BigDecimal.ZERO : latestProduct.getPostage();
        String orderNo = SerialNumberGenerator.generateFlashSaleOrderNo();

        ShopProductCart cart = buildFlashReserveCart(userId, dto.getProductId(), dto.getSkuId(), cartNum, flashSale.getId());
        ShopOrder order = persistFlashSaleReservation(orderNo, userId, cart, flashSale, totalPrice, freightPrice, cartNum);
        cacheFlashSaleReserve(orderNo, order.getId(), dto.getProductId(), dto.getSkuId(), flashSale, cart.getId(), cartNum);

        return assembleOrderCreateVO(orderNo, ORDER_TYPE_FLASH, totalPrice.add(freightPrice));
    }

    private List<Integer> validateAndExtractCartIds(List<Integer> shoppingCartIds) {
        CommonResponseEnum.INVALID.message("购物车数据无效").assertTrue(shoppingCartIds == null || shoppingCartIds.isEmpty());
        List<Integer> distinctCartIds = shoppingCartIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        CommonResponseEnum.INVALID.message("购物车数据无效").assertTrue(distinctCartIds.isEmpty());
        return distinctCartIds;
    }

    private CartSummary summarizeCartItems(List<ShopProductCartAndSkuVO> cartRows) {
        CartSummary summary = new CartSummary();
        if (cartRows == null) {
            return summary;
        }
        int totalNum = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalPostage = BigDecimal.ZERO;
        Map<Long, String> productNames = new HashMap<>();
        Map<Long, BigDecimal> productAmount = new HashMap<>();
        Map<Long, Integer> productCateIds = new HashMap<>();

        for (ShopProductCartAndSkuVO row : cartRows) {
            if (row == null) {
                continue;
            }
            int num = row.getCartNum() == null ? 0 : row.getCartNum();
            BigDecimal price = row.getPrice() == null ? BigDecimal.ZERO : row.getPrice();
            BigDecimal postage = row.getPostage() == null ? BigDecimal.ZERO : row.getPostage();

            totalNum += num;
            totalPrice = totalPrice.add(price.multiply(BigDecimal.valueOf(num)));
            totalPostage = totalPostage.add(postage);

            if (row.getProductId() != null) {
                productNames.put(row.getProductId(), row.getProductName() == null ? "" : row.getProductName());
                productAmount.merge(row.getProductId(), price.multiply(BigDecimal.valueOf(num)), BigDecimal::add);
                if (row.getCateId() != null) {
                    productCateIds.put(row.getProductId(), row.getCateId());
                }
            }
        }

        summary.setTotalNum(totalNum);
        summary.setTotalPrice(totalPrice);
        summary.setTotalPostage(totalPostage);
        summary.setProductNames(productNames);
        summary.setProductAmount(productAmount);
        summary.setProductCateIds(productCateIds);
        return summary;
    }

    private ShopOrder buildNormalPreOrderEntity(String orderNo,
                                              long userId,
                                              ShopUser shopUser,
                                              List<Integer> distinctCartIds,
                                              CartSummary summary) {
        String cartToStr = distinctCartIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return new ShopOrder()
                .setOrderId(orderNo)
                .setUid(userId)
                .setRealName(shopUser == null ? null : shopUser.getRealName())
                .setUserPhone(shopUser == null ? null : shopUser.getPhone())
                .setCartIds(cartToStr)
                .setTotalNum(summary.getTotalNum())
                .setTotalPrice(summary.getTotalPrice())
                .setFreightPrice(summary.getTotalPostage())
                .setPaid(ORDER_PAID_PENDING)
                .setStatus(ShopOrderStatusEnum.CREATED.getCode());
    }

    private ShopProductCart buildFlashReserveCart(long userId, Long productId, Long skuId, int cartNum, Long flashSaleId) {
        ShopProductCart cart = new ShopProductCart();
        cart.setUid(userId);
        cart.setProductId(productId);
        cart.setSkuId(skuId);
        cart.setCartNum(cartNum);
        cart.setSeckillId(Math.toIntExact(flashSaleId));
        cart.setStatus(2);
        return cart;
    }

    private ShopPreOrderVO assembleCreatedPreOrderVO(String orderNo) {
        ShopPreOrderVO preOrderVO = new ShopPreOrderVO();
        preOrderVO.setOrderNo(orderNo);
        preOrderVO.setOrderType(ORDER_TYPE_NORMAL);
        preOrderVO.setRemainingSeconds(0L);
        return preOrderVO;
    }

    private List<ShopCouponAndUserVO> assembleAvailableCoupons(long userId,
                                                             CartSummary summary,
                                                             List<Long> needExpireUserCouponIds) {
        LocalDateTime now = LocalDateTime.now();
        List<ShopProductCouponUser> userCoupons = shopProductCouponUserMapper.selectByUidOrderByCreateTimeDesc(userId);
        Map<Long, ShopProductCouponUser> userCouponByCid = deduplicateLatestUserCoupons(userCoupons);
        if (userCouponByCid.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ShopProductCoupon> couponById = shopProductCouponMapper.selectByIds(userCouponByCid.keySet())
                .stream()
                .collect(Collectors.toMap(ShopProductCoupon::getId, c -> c, (a, b) -> a));

        Set<Long> orderProductIds = new HashSet<>(summary.getProductNames().keySet());
        return userCouponByCid.entrySet().stream()
                .map(entry -> buildMatchedCouponUserVO(entry.getValue(), couponById.get(entry.getKey()),
                        orderProductIds, summary.getProductAmount(), now, needExpireUserCouponIds))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<Long, ShopProductCouponUser> deduplicateLatestUserCoupons(List<ShopProductCouponUser> userCoupons) {
        if (userCoupons == null || userCoupons.isEmpty()) {
            return Collections.emptyMap();
        }
        return userCoupons.stream()
                .filter(uc -> uc.getCid() != null)
                .collect(Collectors.toMap(
                        ShopProductCouponUser::getCid,
                        uc -> uc,
                        (a, b) -> {
                            LocalDateTime ta = a.getCreateTime();
                            LocalDateTime tb = b.getCreateTime();
                            if (ta == null) return b;
                            if (tb == null) return a;
                            return tb.isAfter(ta) ? b : a;
                        }
                ));
    }

    private ShopCouponAndUserVO buildMatchedCouponUserVO(ShopProductCouponUser uc,
                                                       ShopProductCoupon coupon,
                                                       Set<Long> orderProductIds,
                                                       Map<Long, BigDecimal> productAmount,
                                                       LocalDateTime now,
                                                       List<Long> needExpireUserCouponIds) {
        if (coupon == null) {
            return null;
        }
        BigDecimal matchedAmount = calcMatchedAmountForOrder(coupon, orderProductIds, productAmount);
        if (matchedAmount == null) {
            return null;
        }
        BigDecimal minMoney = coupon.getMinMoney() == null ? BigDecimal.ZERO : coupon.getMinMoney();
        if (matchedAmount.compareTo(minMoney) < 0) {
            return null;
        }

        int status = uc.getStatus() == null ? 0 : uc.getStatus();
        boolean expiredByCouponTime = (coupon.getCouponEndTime() != null && now.isAfter(coupon.getCouponEndTime()))
                || (coupon.getUseEndTime() != null && now.isAfter(coupon.getUseEndTime()));
        if (status != 1 && (status == 2 || expiredByCouponTime)) {
            if (status != 2 && expiredByCouponTime && uc.getId() != null) {
                needExpireUserCouponIds.add(uc.getId());
            }
            status = 2;
        }

        ShopCouponAndUserVO vo = new ShopCouponAndUserVO();
        vo.setId(uc.getId());
        vo.setCid(uc.getCid());
        vo.setName(coupon.getName());
        vo.setMoney(coupon.getMoney());
        vo.setMinMoney(coupon.getMinMoney());
        vo.setCouponEndTime(coupon.getCouponEndTime());
        vo.setUid(uc.getUid());
        vo.setType(uc.getType());
        vo.setStatus(status);
        vo.setCreateTime(uc.getCreateTime());
        vo.setUpdateTime(uc.getUpdateTime());
        vo.setUseTime(uc.getUseTime());
        vo.setUseType(uc.getUseType());
        return vo;
    }

    private ShopPreOrderVO assemblePreOrderDetailVO(ShopOrder shopOrder,
                                                  Long defaultAddressId,
                                                  CartSummary summary,
                                                  List<ShopProductCartAndSkuVO> cartRows,
                                                  List<ShopCouponAndUserVO> couponVos,
                                                  long maxUseIntegral,
                                                  long flashRemainingSeconds) {
        BigDecimal couponPrice = shopOrder.getCouponPrice() == null ? BigDecimal.ZERO : shopOrder.getCouponPrice();
        BigDecimal deductionPrice = shopOrder.getDeductionPrice() == null ? BigDecimal.ZERO : shopOrder.getDeductionPrice();
        BigDecimal finalTotal = summary.getTotalPrice().add(summary.getTotalPostage())
                .subtract(couponPrice).subtract(deductionPrice).max(BigDecimal.ZERO);

        ShopPreOrderVO vo = new ShopPreOrderVO();
        vo.setOrderNo(shopOrder.getOrderId());
        vo.setAddressId(defaultAddressId);
        vo.setFreightTotalFee(summary.getTotalPostage().toPlainString());
        vo.setPayFee(summary.getTotalPrice().toPlainString());
        vo.setIsUseIntegral(maxUseIntegral > 0);
        vo.setTotalNum(summary.getTotalNum());
        vo.setUserIntegral((int) Math.min(Integer.MAX_VALUE, maxUseIntegral));
        vo.setIsUseCoupon(couponVos != null && couponVos.stream().anyMatch(c -> c.getStatus() != null && c.getStatus() == 0));
        vo.setCouponPrice(couponPrice.toPlainString());
        vo.setDeductionPrice(deductionPrice.toPlainString());
        vo.setFinalTotalFee(finalTotal.toPlainString());
        vo.setOrderInfoList(cartRows);
        vo.setPlatCouponUserList(couponVos);
        vo.setOrderType(resolveOrderType(shopOrder));
        vo.setRemainingSeconds(resolveOrderType(shopOrder) == ORDER_TYPE_FLASH ? Math.max(flashRemainingSeconds, 0L) : 0L);
        return vo;
    }

    private CouponChoice resolveCouponForRecalculate(ShopPreOrderVO vo, ShopOrderDTO.Recalculate dto, BigDecimal goodsTotal) {
        CouponChoice choice = new CouponChoice();
        Long selectedCouponUserId = dto.getUserCouponId() != null ? dto.getUserCouponId() : dto.getPlatUserCouponId();
        if (selectedCouponUserId == null || selectedCouponUserId <= 0) {
            return choice;
        }
        ShopCouponAndUserVO selectedCoupon = vo.getPlatCouponUserList() == null ? null : vo.getPlatCouponUserList().stream()
                .filter(c -> c.getId() != null && c.getId().equals(selectedCouponUserId))
                .findFirst()
                .orElse(null);
        CommonResponseEnum.INVALID.message("优惠券不可用，请刷新后重试")
                .assertTrue(selectedCoupon == null || selectedCoupon.getStatus() == null || selectedCoupon.getStatus() != 0);
        BigDecimal couponMoney = selectedCoupon.getMoney() == null ? BigDecimal.ZERO : selectedCoupon.getMoney();
        choice.setSelectedCoupon(selectedCoupon);
        choice.setCouponPrice(couponMoney.min(goodsTotal).max(BigDecimal.ZERO));
        return choice;
    }

    private CouponConfirmResult resolveAndValidateCouponForCreate(long userId,
                                                                  Integer platUserCouponId,
                                                                  BigDecimal goodsTotal,
                                                                  CartSummary summary) {
        CouponConfirmResult result = new CouponConfirmResult();
        if (platUserCouponId == null || platUserCouponId <= 0) {
            return result;
        }

        ShopProductCouponUser couponUser = shopProductCouponUserMapper.selectAvailableByIdAndUid(platUserCouponId.longValue(), userId);
        CommonResponseEnum.INVALID.message("用户优惠券不存在或不可用").assertNull(couponUser);

        ShopProductCoupon coupon = shopProductCouponMapper.selectOneById(couponUser.getCid());
        CommonResponseEnum.INVALID.message("优惠券不存在或不可用").assertNull(coupon);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime useStartTime = coupon.getUseStartTime();
        LocalDateTime useEndTime = coupon.getUseEndTime();
        CommonResponseEnum.INVALID
                .message("优惠券未生效，当前时间=" + now + ", useStartTime=" + useStartTime)
                .assertTrue(useStartTime != null && now.isBefore(useStartTime));
        CommonResponseEnum.INVALID
                .message("优惠券已过期，当前时间=" + now + ", useEndTime=" + useEndTime)
                .assertTrue(useEndTime != null && now.isAfter(useEndTime));

        Set<Long> orderProductIds = new HashSet<>(summary.getProductNames().keySet());
        BigDecimal matchedAmount = calcMatchedAmountForOrder(coupon, orderProductIds, summary.getProductAmount());
        CommonResponseEnum.INVALID.message("优惠券不适用于当前订单商品").assertNull(matchedAmount);

        BigDecimal minMoney = coupon.getMinMoney() == null ? BigDecimal.ZERO : coupon.getMinMoney();
        CommonResponseEnum.INVALID.message("订单金额未达到优惠券门槛").assertTrue(matchedAmount.compareTo(minMoney) < 0);

        BigDecimal couponMoney = coupon.getMoney() == null ? BigDecimal.ZERO : coupon.getMoney();
        result.setCoupon(coupon);
        result.setUsedCouponUserId(couponUser.getId());
        result.setCouponDiscount(couponMoney.min(matchedAmount).min(goodsTotal).max(BigDecimal.ZERO));
        return result;
    }

    private IntegralDeduction calculateIntegralDeduction(long userId, boolean useIntegralFlag, BigDecimal remainForIntegral) {
        IntegralDeduction deduction = new IntegralDeduction();
        if (!useIntegralFlag) {
            return deduction;
        }
        ShopUser shopUser = shopUserService.getById(userId);
        long currentIntegral = shopUser == null || shopUser.getIntegral() == null ? 0L : shopUser.getIntegral();
        BigDecimal effectiveRemain = remainForIntegral.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remainForIntegral;
        long maxIntegralByMoney = effectiveRemain.multiply(BigDecimal.valueOf(ORDER_INTEGRAL_DEDUCTION_RATIO))
                .setScale(0, RoundingMode.DOWN)
                .longValue();
        long useIntegral = Math.min(currentIntegral, Math.max(0L, maxIntegralByMoney));
        BigDecimal deductionPrice = BigDecimal.valueOf(useIntegral)
                .divide(BigDecimal.valueOf(ORDER_INTEGRAL_DEDUCTION_RATIO), 2, RoundingMode.DOWN);
        deduction.setUseIntegral(useIntegral);
        deduction.setDeductionPrice(deductionPrice);
        return deduction;
    }

    private void applyRecalculatedFieldsToVO(ShopPreOrderVO vo,
                                             Long selectedAddressId,
                                             BigDecimal payAfterDiscount,
                                             BigDecimal finalTotal,
                                             boolean isUseIntegral,
                                             CouponChoice couponChoice,
                                             IntegralDeduction integralDeduction) {
        vo.setAddressId(selectedAddressId);
        vo.setPayFee(payAfterDiscount.toPlainString());
        vo.setIsUseIntegral(isUseIntegral && integralDeduction.getUseIntegral() > 0);
        vo.setUserIntegral((int) Math.min(Integer.MAX_VALUE, integralDeduction.getUseIntegral()));
        vo.setIsUseCoupon(couponChoice.getSelectedCoupon() != null && couponChoice.getCouponPrice().compareTo(BigDecimal.ZERO) > 0);
        vo.setCouponPrice(couponChoice.getCouponPrice().toPlainString());
        vo.setDeductionPrice(integralDeduction.getDeductionPrice().toPlainString());
        vo.setFinalTotalFee(finalTotal.toPlainString());
    }

    private ShopOrder buildRecalculateOrderUpdate(Long orderId,
                                                ShopUserAddress selectedAddress,
                                                CouponChoice couponChoice,
                                                IntegralDeduction integralDeduction,
                                                BigDecimal finalTotal) {
        ShopOrder update = new ShopOrder();
        update.setId(orderId);
        update.setCouponId(couponChoice.getSelectedCoupon() == null ? null : couponChoice.getSelectedCoupon().getCid());
        update.setCouponPrice(couponChoice.getCouponPrice());
        update.setUseIntegral((int) Math.min(Integer.MAX_VALUE, integralDeduction.getUseIntegral()));
        update.setDeductionPrice(integralDeduction.getDeductionPrice());
        update.setPayPrice(finalTotal);
        if (selectedAddress != null) {
            update.setRealName(selectedAddress.getRealName());
            update.setUserPhone(selectedAddress.getPhone());
            update.setUserAddress(joinAddress(selectedAddress));
        }
        return update;
    }

    private ShopOrder buildOrderConfirmUpdate(Long orderId,
                                            ShopUserAddress address,
                                            CouponConfirmResult couponResult,
                                            IntegralDeduction integralDeduction,
                                            BigDecimal payPrice) {
        ShopOrder update = new ShopOrder();
        update.setId(orderId);
        update.setRealName(address.getRealName());
        update.setUserPhone(address.getPhone());
        update.setUserAddress(joinAddressWithSpaces(address));
        update.setUseIntegral((int) Math.min(Integer.MAX_VALUE, integralDeduction.getUseIntegral()));
        update.setDeductionPrice(integralDeduction.getDeductionPrice());
        update.setCouponId(couponResult.getCoupon() == null ? null : couponResult.getCoupon().getId());
        update.setCouponPrice(couponResult.getCouponDiscount());
        update.setPayPrice(payPrice);
        return update;
    }

    private ShopOrderCreateVO assembleOrderCreateVO(String orderNo, int orderType, BigDecimal payPrice) {
        ShopOrderCreateVO vo = new ShopOrderCreateVO();
        vo.setOrderNo(orderNo);
        vo.setOrderType(orderType);
        vo.setPayPrice(payPrice == null ? "0" : payPrice.toPlainString());
        return vo;
    }

    private ShopOrderPaymentInfoVO assemblePaymentInfoVO(ShopOrder order, ShopUser shopUser, long remainingSeconds) {
        BigDecimal nowMoney = shopUser == null || shopUser.getNowMoney() == null ? BigDecimal.ZERO : shopUser.getNowMoney();
        int paid = normalizePaidStatus(order.getPaid());

        ShopOrderPaymentInfoVO vo = new ShopOrderPaymentInfoVO();
        vo.setOrderNo(order.getOrderId());
        vo.setPayPrice(resolvePayPrice(order).toPlainString());
        vo.setTotalNum(order.getTotalNum());
        vo.setPaid(paid);
        vo.setOrderType(resolveOrderType(order));
        vo.setPayType(order.getPayType());
        vo.setRemainingSeconds(Math.max(remainingSeconds, 0L));
        vo.setNowMoney(nowMoney.toPlainString());
        vo.setCanPay(paid == ORDER_PAID_PENDING && remainingSeconds > 0);
        vo.setEnableWxpay(shopPaymentProperties.isEnableWxpay());
        vo.setEnableAlipay(shopPaymentProperties.isEnableAlipay());
        vo.setEnableYue(shopPaymentProperties.isEnableYue());
        return vo;
    }

    private ShopPaymentVO assemblePaymentVO(String orderNo, String payType) {
        ShopPaymentVO vo = new ShopPaymentVO();
        vo.setOrderNo(orderNo);
        vo.setPayType(payType);
        vo.setStatus(Boolean.TRUE);
        return vo;
    }

    private ShopFrontOrderVO assembleFrontOrderSummary(ShopOrder order) {
        if (order == null) {
            return null;
        }
        long remainingSeconds = refreshFrontOrderPaymentState(order);
        List<ShopProductCartAndSkuVO> cartRows = fetchOrderCartRows(parseOrderCartIdsQuietly(order.getCartIds()));
        List<ShopFrontOrderProductVO> productList = cartRows.stream().map(this::toFrontOrderProduct).collect(Collectors.toList());
        ShopOrderAfterSale afterSale = findOrderAfterSale(order.getId());

        ShopFrontOrderVO vo = new ShopFrontOrderVO();
        vo.setOrderNo(order.getOrderId());
        vo.setPaid(normalizePaidStatus(order.getPaid()));
        vo.setStatus(order.getStatus());
        vo.setDisplayTab(resolveFrontDisplayTab(order));
        vo.setDisplayStatus(resolveFrontDisplayStatus(order, afterSale));
        vo.setOrderType(resolveOrderType(order));
        vo.setOrderTypeLabel(resolveOrderTypeLabel(order));
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setPayPrice(resolvePayPrice(order).toPlainString());
        vo.setTotalNum(order.getTotalNum() == null
                ? productList.stream().mapToInt(item -> item.getCartNum() == null ? 0 : item.getCartNum()).sum()
                : order.getTotalNum());
        vo.setFirstProductId(productList.isEmpty() ? null : productList.get(0).getProductId());
        vo.setCanPay("pending_payment".equals(vo.getDisplayTab()) && remainingSeconds > 0);
        vo.setCanConfirmReceipt("pending_receipt".equals(vo.getDisplayTab()));
        vo.setCanEvaluate("pending_review".equals(vo.getDisplayTab()) && !hasBlockingAfterSale(afterSale));
        vo.setCanBuyAgain(!productList.isEmpty());
        vo.setCanDelete("cancelled".equals(vo.getDisplayTab()) || "completed".equals(vo.getDisplayTab()));
        vo.setCanApplyAfterSale(canApplyAfterSale(order, afterSale));
        vo.setAfterSaleId(afterSale == null ? null : afterSale.getId());
        vo.setAfterSaleStatus(afterSale == null ? null : afterSale.getStatus());
        vo.setAfterSaleStatusText(resolveAfterSaleStatusText(afterSale));
        vo.setOrderInfoList(productList);
        return vo;
    }

    private ShopFrontOrderDetailVO assembleFrontOrderDetailVO(ShopOrder order,
                                                            ShopFrontOrderVO summary,
                                                            List<ShopOrderStatusVO> orderStatusList) {
        ShopFrontOrderDetailVO detail = new ShopFrontOrderDetailVO();
        BeanCopyUtils.copy(summary, detail);
        detail.setRealName(order.getRealName());
        detail.setUserPhone(order.getUserPhone());
        detail.setUserAddress(order.getUserAddress());
        detail.setPayType(order.getPayType());
        detail.setFreightTotalFee(order.getFreightPrice() == null ? "0" : order.getFreightPrice().toPlainString());
        detail.setExpressNo(order.getExpressNo());
        if (order.getDeliveryId() != null) {
            ShopExpress express = shopExpressService.getById(order.getDeliveryId());
            if (express != null) {
                detail.setExpressName(express.getExpressName());
            }
        }
        detail.setOrderStatusList(orderStatusList);
        return detail;
    }

    private ShopOrderAdminDetailVO assembleAdminOrderDetailVO(ShopOrder order,
                                                            List<ShopProductCartAndSkuVO> cartRows,
                                                            List<ShopOrderStatusVO> orderStatusList) {
        String payPriceText = order.getPayPrice() == null
                ? (order.getTotalPrice() == null ? "0" : order.getTotalPrice().toPlainString())
                : order.getPayPrice().toPlainString();

        ShopOrderAdminDetailVO vo = new ShopOrderAdminDetailVO();
        vo.setOrderNo(order.getOrderId());
        vo.setAddressId(null);
        vo.setFreightTotalFee(order.getFreightPrice() == null ? "0" : order.getFreightPrice().toPlainString());
        vo.setPayFee(payPriceText);
        vo.setIsUseIntegral(order.getUseIntegral() != null && order.getUseIntegral() > 0);
        vo.setUserIntegral(order.getUseIntegral() == null ? 0 : order.getUseIntegral());
        vo.setIsUseCoupon(order.getCouponId() != null);
        vo.setFinalTotalFee(payPriceText);
        vo.setTotalNum(order.getTotalNum());
        vo.setOrderType(resolveOrderType(order));
        vo.setRemainingSeconds(0L);
        vo.setOrderInfoList(cartRows);
        vo.setPlatCouponUserList(Collections.emptyList());
        vo.setOrderStatusList(orderStatusList);
        vo.setDeliveryId(order.getDeliveryId());
        vo.setExpressNo(order.getExpressNo());
        if (order.getDeliveryId() != null) {
            ShopExpress express = shopExpressService.getById(order.getDeliveryId());
            if (express != null) {
                vo.setExpressCode(express.getCode());
                vo.setExpressName(express.getExpressName());
            }
        }
        return vo;
    }

    private List<ShopProductComment> buildProductComments(ShopOrder order, long userId, int score, String contentText) {
        List<ShopProductCartAndSkuVO> cartRows = fetchOrderCartRows(parseOrderCartIdsQuietly(order.getCartIds()));
        CommonResponseEnum.INVALID.message("订单商品不存在，无法提交评价").assertTrue(cartRows == null || cartRows.isEmpty());

        Set<Long> productIds = cartRows.stream()
                .map(ShopProductCartAndSkuVO::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        CommonResponseEnum.INVALID.message("订单商品不存在，无法提交评价").assertTrue(productIds.isEmpty());

        List<ShopProductComment> comments = new ArrayList<>(productIds.size());
        for (Long productId : productIds) {
            ShopProductComment comment = new ShopProductComment();
            comment.setUid(userId);
            comment.setOid(order.getId());
            comment.setProductId(productId);
            comment.setProductScore(score);
            comment.setServiceScore(score);
            comment.setComment(contentText);
            comment.setPic(null);
            comment.setIsDel(0);
            comments.add(comment);
        }
        CommonResponseEnum.INVALID.message("生成评价数据失败").assertTrue(comments.isEmpty());
        return comments;
    }

    private ShopExpressInfo buildDeliverExpressInfo(ShopOrder order, ShopExpressTemplate expressTemplate, String expressNo) {
        ShopExpressInfo expressInfo = new ShopExpressInfo();
        expressInfo.setOid(order.getId());
        expressInfo.setExpressTmpId(expressTemplate.getId());
        expressInfo.setExpressNo(expressNo);
        expressInfo.setRealName(order.getRealName());
        expressInfo.setUserPhone(order.getUserPhone());
        expressInfo.setUserAddress(order.getUserAddress());
        expressInfo.setStatus(1);
        return expressInfo;
    }

    private ShopOrder buildDeliverOrderUpdate(Long orderId, Long expressId, String expressNo) {
        ShopOrder update = new ShopOrder();
        update.setId(orderId);
        update.setDeliveryId(expressId);
        update.setExpressNo(expressNo);
        update.setStatus(1);
        return update;
    }

    // ==================== 三、数据库读取与持久化写入层 ====================

    private List<ShopProductCartAndSkuVO> loadAndVerifyActiveCarts(long userId, List<Integer> distinctCartIds) {
        List<ShopProductCartAndSkuVO> shopProductCarts = mapper.selectOrderCartWithSku(distinctCartIds, userId, 0);
        if (shopProductCarts.size() != distinctCartIds.size()) {
            Set<Long> foundCartIds = shopProductCarts.stream()
                    .map(ShopProductCartAndSkuVO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            List<Integer> missingCartIds = distinctCartIds.stream()
                    .filter(id -> !foundCartIds.contains(Long.valueOf(id)))
                    .toList();
            CommonResponseEnum.INVALID.message("购物车数据不一致，请刷新后重试，先需要添加购物车，缺失购物车ID=" + missingCartIds)
                    .assertTrue(true);
        }
        return shopProductCarts;
    }

    private List<ShopProductCartAndSkuVO> loadAndSortPreOrderCartRows(long userId, List<Long> distinctCartIds) {
        List<ShopProductCartAndSkuVO> cartRows = mapper.selectOrderCartWithSku(distinctCartIds, userId, null);
        CommonResponseEnum.INVALID.message("购物车数据不一致，请刷新后重试")
                .assertTrue(cartRows.size() != distinctCartIds.size());
        for (ShopProductCartAndSkuVO row : cartRows) {
            CommonResponseEnum.INVALID.message("购物车数据无效")
                    .assertTrue(row.getProductId() == null || row.getCartNum() == null);
        }
        sortCartRowsByIds(cartRows, distinctCartIds);
        return cartRows;
    }

    private void persistNormalPreOrder(ShopOrder order, long userId, List<Integer> distinctCartIds) {
        save(order);
        saveOrderStatusLog(order.getId(), ShopOrderStatusConstant.CHANGE_TYPE_CREATE_ORDER, ShopOrderStatusConstant.CHANGE_MESSAGE_CREATE_ORDER);

        int affectRows = shopProductCartMapper.markRemovedByIdsAndUid(userId, distinctCartIds);
        CommonResponseEnum.INVALID.message("更新购物车状态失败，请刷新后重试")
                .assertTrue(affectRows != distinctCartIds.size());
    }

    private void deductFlashStockOptimistically(ShopProductAttrValue latestSku,
                                                int skuStock,
                                                ShopProduct latestProduct,
                                                int aggregatedStock,
                                                int cartNum) {
        int nextSkuVersion = (latestSku.getVersion() == null ? 0 : latestSku.getVersion()) + 1;
        int skuAffected = shopProductAttrValueMapper.deductStockOptimistic(
                latestSku.getId(), skuStock - cartNum, nextSkuVersion, cartNum, latestSku.getVersion());
        CommonResponseEnum.INVALID.message("秒杀商品库存不足，请重试").assertTrue(skuAffected != 1);

        int nextProductVersion = (latestProduct.getVersion() == null ? 0 : latestProduct.getVersion()) + 1;
        int productAffected = shopProductMapper.updateStockOptimistic(
                latestProduct.getId(), Math.max(aggregatedStock - cartNum, 0), nextProductVersion, latestProduct.getVersion());
        CommonResponseEnum.INVALID.message("秒杀商品库存不足，请重试").assertTrue(productAffected != 1);
    }

    private ShopOrder persistFlashSaleReservation(String orderNo,
                                                long userId,
                                                ShopProductCart cart,
                                                ShopFlashSale flashSale,
                                                BigDecimal totalPrice,
                                                BigDecimal freightPrice,
                                                int cartNum) {
        shopProductCartMapper.insert(cart);

        ShopOrder order = new ShopOrder()
                .setOrderId(orderNo)
                .setUid(userId)
                .setRealName("")
                .setUserPhone("")
                .setCartIds(String.valueOf(cart.getId()))
                .setTotalNum(cartNum)
                .setTotalPrice(totalPrice)
                .setFreightPrice(freightPrice)
                .setPayPrice(totalPrice.add(freightPrice))
                .setPaid(ORDER_PAID_PENDING)
                .setStatus(ShopOrderStatusEnum.CREATED.getCode())
                .setSeckillId(Math.toIntExact(flashSale.getId()));
        save(order);
        saveOrderStatusLog(order.getId(), ShopOrderStatusConstant.CHANGE_TYPE_FLASH_RESERVE, ShopOrderStatusConstant.CHANGE_MESSAGE_FLASH_RESERVE);
        return order;
    }

    private void persistExpiredUserCoupons(List<Long> needExpireUserCouponIds, LocalDateTime now) {
        if (needExpireUserCouponIds == null || needExpireUserCouponIds.isEmpty()) {
            return;
        }
        for (Long id : needExpireUserCouponIds) {
            ShopProductCouponUser update = new ShopProductCouponUser();
            update.setId(id);
            update.setStatus(2);
            update.setUpdateTime(now);
            shopProductCouponUserMapper.update(update);
        }
    }

    private void persistOrderConfirmAndCoupon(ShopOrder order, ShopOrder updateEntity, long userId, Long usedCouponUserId) {
        updateById(updateEntity);
        if (usedCouponUserId != null) {
            int affectRows = shopProductCouponUserMapper.markUsedByIdAndUid(usedCouponUserId, userId, LocalDateTime.now());
            CommonResponseEnum.INVALID.message("更新优惠券状态失败，请重试").assertTrue(affectRows != 1);
        }
    }

    private void persistPaymentSettlement(ShopOrder order,
                                          long userId,
                                          String payType,
                                          BigDecimal payPrice,
                                          BigDecimal remainMoney,
                                          int useIntegral,
                                          int remainIntegral) {
        ShopUser userUpdate = new ShopUser();
        userUpdate.setUid(userId);
        userUpdate.setNowMoney(remainMoney);
        userUpdate.setIntegral(remainIntegral);
        boolean userUpdated = shopUserService.updateById(userUpdate);
        CommonResponseEnum.INVALID.message("账户资产扣减失败，请重试").assertTrue(!userUpdated);

        if (resolveOrderType(order) != ORDER_TYPE_FLASH) {
            deductOrderStock(order, userId);
        }

        addUserPayBill(order, userId, payPrice, remainMoney);
        if (useIntegral > 0) {
            addUserUseIntegralBill(order, userId, useIntegral, remainIntegral);
        }

        LocalDateTime now = LocalDateTime.now();
        ShopOrder orderUpdate = new ShopOrder();
        orderUpdate.setId(order.getId());
        orderUpdate.setPaid(ORDER_PAID_SUCCESS);
        orderUpdate.setPayTime(now);
        orderUpdate.setPayType(payType);
        boolean orderUpdated = updateById(orderUpdate);
        CommonResponseEnum.INVALID.message("订单支付状态更新失败，请重试").assertTrue(!orderUpdated);

        clearOrderPaymentCountdown(order.getOrderId());
        order.setStatus(ShopOrderStatusEnum.PAID.getCode());
        order.setPaid(ORDER_PAID_SUCCESS);
        order.setPayTime(now);
        order.setPayType(payType);
        if (resolveOrderType(order) == ORDER_TYPE_FLASH) {
            saveOrderStatusLog(order.getId(), ShopOrderStatusConstant.CHANGE_TYPE_PAY_SUCCESS, "秒杀订单支付成功");
        } else {
            addOrderStatusLog(order, ShopOrderStatusEnum.PAID.getCode());
        }
    }

    private void persistConfirmReceipt(ShopOrder order, long userId, int gainIntegral) {
        ShopOrder update = new ShopOrder();
        update.setId(order.getId());
        update.setStatus(2);
        update.setGainIntegral(gainIntegral);
        boolean updated = updateById(update);
        CommonResponseEnum.INVALID.message("确认收货失败，请稍后重试").assertTrue(!updated);

        if (gainIntegral > 0) {
            ShopUser user = shopUserService.getById(userId);
            CommonResponseEnum.INVALID_USER.assertNull(user);

            int currentIntegral = (user.getIntegral() == null ? 0 : user.getIntegral()) + gainIntegral;
            ShopUser userUpdate = new ShopUser();
            userUpdate.setUid(userId);
            userUpdate.setIntegral(currentIntegral);
            boolean userUpdated = shopUserService.updateById(userUpdate);
            CommonResponseEnum.INVALID.message("更新用户积分失败，请稍后重试").assertTrue(!userUpdated);

            addUserGainIntegralBill(order, userId, gainIntegral, currentIntegral);
        }

        saveOrderStatusLog(order.getId(), ShopOrderStatusConstant.CHANGE_TYPE_USER_TAKE_DELIVERY,
                ShopOrderStatusConstant.CHANGE_MESSAGE_USER_TAKE_DELIVERY);
    }

    private void persistOrderEvaluation(ShopOrder order, List<ShopProductComment> comments) {
        for (ShopProductComment comment : comments) {
            int rows = shopProductCommentMapper.insert(comment);
            CommonResponseEnum.INVALID.message("写入商品评价失败，请稍后重试").assertTrue(rows <= 0);
        }

        ShopOrder update = new ShopOrder();
        update.setId(order.getId());
        update.setStatus(3);
        boolean updated = updateById(update);
        CommonResponseEnum.INVALID.message("提交评价失败，请稍后重试").assertTrue(!updated);

        saveOrderStatusLog(order.getId(), ShopOrderStatusConstant.CHANGE_TYPE_ORDER_EVAL, ShopOrderStatusConstant.CHANGE_MESSAGE_ORDER_EVAL);
    }

    private void persistDeliverAction(Long orderId, Long expressId, ShopExpressInfo expressInfo, ShopOrder orderUpdate) {
        shopExpressInfoMapper.insert(expressInfo);
        updateById(orderUpdate);
        saveOrderStatusLog(orderId, ShopOrderStatusConstant.CHANGE_TYPE_DELIVERY,
                String.format(ShopOrderStatusConstant.CHANGE_MESSAGE_DELIVERY_TEMPLATE, resolveExpressName(expressId)));
    }

    private void addUserPayBill(ShopOrder order, long userId, BigDecimal payPrice, BigDecimal remainMoney) {
        ShopUserBill userBill = new ShopUserBill();
        userBill.setUid(userId);
        userBill.setLinkId(order.getId() == null ? order.getOrderId() : String.valueOf(order.getId()));
        userBill.setPm(ShopPaymentConstant.PM_PAY);
        userBill.setTitle("购买商品");
        userBill.setCategory(ShopPaymentConstant.CATEGORY_NOW_MONEY);
        userBill.setType(ShopPaymentConstant.TYPE_PAY_ORDER);
        userBill.setNumber(payPrice);
        userBill.setBalance(remainMoney);
        userBill.setMark("支付" + payPrice.toPlainString() + "元购买商品");
        userBill.setStatus(1);
        boolean saved = shopUserBillService.save(userBill);
        CommonResponseEnum.INVALID.message("写入用户账单失败，请重试").assertTrue(!saved);
    }

    private void addUserUseIntegralBill(ShopOrder order, long userId, int useIntegral, int remainIntegral) {
        ShopUserIntegralBill integralBill = new ShopUserIntegralBill();
        integralBill.setUid(userId);
        integralBill.setLinkId(order.getId() == null ? order.getOrderId() : String.valueOf(order.getId()));
        integralBill.setPm(ShopPaymentConstant.PM_PAY);
        integralBill.setTitle("订单抵扣积分");
        integralBill.setCategory(ShopPaymentConstant.CATEGORY_INTEGRAL);
        integralBill.setType(ShopPaymentConstant.TYPE_USE_INTEGRAL_ORDER);
        integralBill.setNumber(useIntegral);
        integralBill.setBalance(remainIntegral);
        integralBill.setMark("订单支付使用" + useIntegral + "积分");
        integralBill.setStatus(1);
        boolean saved = shopUserIntegralBillService.save(integralBill);
        CommonResponseEnum.INVALID.message("写入积分流水失败，请重试").assertTrue(!saved);
    }

    private void addUserGainIntegralBill(ShopOrder order, long userId, int gainIntegral, int currentIntegral) {
        ShopUserIntegralBill integralBill = new ShopUserIntegralBill();
        integralBill.setUid(userId);
        integralBill.setLinkId(order.getId() == null ? order.getOrderId() : String.valueOf(order.getId()));
        integralBill.setPm(ShopPaymentConstant.PM_REFUND);
        integralBill.setTitle("订单收货赠送积分");
        integralBill.setCategory(ShopPaymentConstant.CATEGORY_INTEGRAL);
        integralBill.setType(ShopPaymentConstant.TYPE_GAIN_INTEGRAL_ORDER);
        integralBill.setNumber(gainIntegral);
        integralBill.setBalance(currentIntegral);
        integralBill.setMark("订单确认收货获得" + gainIntegral + "积分");
        integralBill.setStatus(1);
        boolean saved = shopUserIntegralBillService.save(integralBill);
        CommonResponseEnum.INVALID.message("写入积分流水失败，请重试").assertTrue(!saved);
    }

    private void deductOrderStock(ShopOrder order, long userId) {
        List<Long> cartIds = parseOrderCartIds(order.getCartIds());
        List<ShopProductCart> carts = shopProductCartMapper.selectByUidAndIds(userId, cartIds);
        CommonResponseEnum.INVALID.message("订单购物车数据不一致，请刷新后重试")
                .assertTrue(carts == null || carts.size() != cartIds.size());

        Map<Long, Integer> skuDeductMap = new HashMap<>();
        for (ShopProductCart cart : carts) {
            CommonResponseEnum.INVALID.message("订单购物车数据无效")
                    .assertTrue(cart.getSkuId() == null || cart.getCartNum() == null || cart.getCartNum() <= 0);
            skuDeductMap.merge(cart.getSkuId(), cart.getCartNum(), Integer::sum);
        }

        Map<Long, Integer> productDeductMap = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : skuDeductMap.entrySet()) {
            Long skuId = entry.getKey();
            Integer deductNum = entry.getValue();

            ShopProductAttrValue sku = shopProductAttrValueService.getById(skuId);
            CommonResponseEnum.INVALID.message("商品规格不存在或已下架").assertNull(sku);

            int skuStock = sku.getStock() == null ? 0 : sku.getStock();
            CommonResponseEnum.INVALID.message("商品库存不足").assertTrue(skuStock < deductNum);

            ShopProductAttrValue skuUpdate = new ShopProductAttrValue();
            skuUpdate.setId(skuId);
            skuUpdate.setStock(skuStock - deductNum);
            boolean skuUpdated = shopProductAttrValueService.updateById(skuUpdate);
            CommonResponseEnum.INVALID.message("扣减商品规格库存失败，请重试").assertTrue(!skuUpdated);

            CommonResponseEnum.INVALID.message("商品数据异常，缺少商品ID").assertNull(sku.getProductId());
            productDeductMap.merge(sku.getProductId(), deductNum, Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : productDeductMap.entrySet()) {
            Long productId = entry.getKey();
            Integer deductNum = entry.getValue();

            ShopProduct product = shopProductService.getById(productId);
            CommonResponseEnum.INVALID.message("商品不存在或已下架").assertNull(product);

            int productStock = product.getStock() == null ? 0 : product.getStock();
            CommonResponseEnum.INVALID.message("商品库存不足").assertTrue(productStock < deductNum);

            ShopProduct productUpdate = new ShopProduct();
            productUpdate.setId(productId);
            productUpdate.setStock(productStock - deductNum);
            boolean productUpdated = shopProductService.updateById(productUpdate);
            CommonResponseEnum.INVALID.message("扣减商品库存失败，请重试").assertTrue(!productUpdated);
        }
    }

    private void releaseFlashReservedStock(ShopOrder order, String changeMessage) {
        if (order == null || resolveOrderType(order) != ORDER_TYPE_FLASH) {
            return;
        }
        List<Long> cartIds = parseOrderCartIdsQuietly(order.getCartIds());
        if (cartIds.isEmpty()) {
            return;
        }
        List<ShopProductCart> carts = shopProductCartMapper.selectByIds(cartIds);
        if (carts == null || carts.isEmpty()) {
            return;
        }

        Map<Long, Integer> productRestoreMap = new HashMap<>();
        for (ShopProductCart cart : carts) {
            if (cart.getSkuId() == null || cart.getCartNum() == null || cart.getCartNum() <= 0) {
                continue;
            }
            ShopProductAttrValue sku = shopProductAttrValueService.getById(cart.getSkuId());
            if (sku != null) {
                int newStock = (sku.getStock() == null ? 0 : sku.getStock()) + cart.getCartNum();
                int nextVersion = (sku.getVersion() == null ? 0 : sku.getVersion()) + 1;
                shopProductAttrValueMapper.restoreStockOptimistic(sku.getId(), newStock, nextVersion, sku.getVersion());
                if (sku.getProductId() != null) {
                    productRestoreMap.merge(sku.getProductId(), cart.getCartNum(), Integer::sum);
                }
            }
        }

        for (Map.Entry<Long, Integer> entry : productRestoreMap.entrySet()) {
            ShopProduct product = shopProductService.getById(entry.getKey());
            if (product == null) {
                continue;
            }
            int newStock = (product.getStock() == null ? 0 : product.getStock()) + entry.getValue();
            int nextVersion = (product.getVersion() == null ? 0 : product.getVersion()) + 1;
            shopProductMapper.updateStockOptimistic(product.getId(), newStock, nextVersion, product.getVersion());
        }

        saveOrderStatusLog(order.getId(), ShopOrderStatusConstant.CHANGE_TYPE_FLASH_RELEASE, changeMessage);
        redisTemplate.delete(buildFlashReserveKey(order.getOrderId()));
    }

    // ==================== 四、状态流转、倒计时与辅助方法 ====================

    private ShopOrder requireFrontOrder(String orderNo, long userId) {
        ShopOrder order = mapper.selectByOrderNoAndUid(orderNo, userId);
        CommonResponseEnum.INVALID.message("订单不存在").assertNull(order);
        return order;
    }

    private ShopUserAddress resolveOptionalUserAddress(long userId, Long addressId) {
        if (addressId == null || addressId <= 0) {
            return null;
        }
        return requireUserAddress(userId, addressId);
    }

    private ShopUserAddress requireUserAddress(long userId, Long addressId) {
        ShopUserAddress address = shopUserAddressMapper.selectActiveByIdAndUid(addressId, userId);
        CommonResponseEnum.INVALID.message("收货地址不存在").assertNull(address);
        return address;
    }

    private Long parseAddressId(String rawAddressId) {
        String trimmed = rawAddressId == null ? "" : rawAddressId.trim();
        CommonResponseEnum.INVALID.message("收货地址不存在").assertTrue(!trimmed.matches("\\d+"));
        return Long.valueOf(trimmed);
    }

    private int resolveAggregatedProductStock(Long productId, ShopProduct latestProduct) {
        List<ShopProductAttrValue> productSkuList = shopProductAttrValueService.getListByProductId(productId);
        int productStock = latestProduct.getStock() == null ? 0 : latestProduct.getStock();
        if (productSkuList == null || productSkuList.isEmpty()) {
            return productStock;
        }
        return productSkuList.stream()
                .map(ShopProductAttrValue::getStock)
                .filter(Objects::nonNull)
                .mapToInt(stock -> Math.max(stock, 0))
                .sum();
    }

    private long resolveMaxUsableIntegral(long userId, BigDecimal totalPrice) {
        ShopUser shopUser = shopUserService.getById(userId);
        int userIntegral = shopUser == null || shopUser.getIntegral() == null ? 0 : shopUser.getIntegral();
        long maxIntegralByMoney = totalPrice.multiply(BigDecimal.valueOf(ORDER_INTEGRAL_DEDUCTION_RATIO))
                .setScale(0, RoundingMode.DOWN)
                .longValue();
        return Math.min((long) userIntegral, Math.max(0L, maxIntegralByMoney));
    }

    private int resolveOrderGainIntegral(ShopOrder order) {
        if (order == null) {
            return 0;
        }
        List<ShopProductCartAndSkuVO> cartRows = fetchOrderCartRows(parseOrderCartIdsQuietly(order.getCartIds()));
        if (cartRows.isEmpty()) {
            return 0;
        }

        BigDecimal totalGainIntegral = BigDecimal.ZERO;
        for (ShopProductCartAndSkuVO cartRow : cartRows) {
            if (cartRow.getCartNum() == null || cartRow.getCartNum() <= 0) {
                continue;
            }
            BigDecimal productGiveIntegral = cartRow.getGiveIntegral() == null ? BigDecimal.ZERO : cartRow.getGiveIntegral();
            if (productGiveIntegral.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            totalGainIntegral = totalGainIntegral.add(productGiveIntegral.multiply(BigDecimal.valueOf(cartRow.getCartNum())));
        }

        long gainIntegral = totalGainIntegral.setScale(0, RoundingMode.DOWN).longValue();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, gainIntegral));
    }

    private void cacheFlashSaleReserve(String orderNo,
                                       Long orderId,
                                       Long productId,
                                       Long skuId,
                                       ShopFlashSale flashSale,
                                       Long cartId,
                                       int cartNum) {
        FlashReserveCache cache = new FlashReserveCache();
        cache.setOrderNo(orderNo);
        cache.setOrderId(orderId);
        cache.setProductId(productId);
        cache.setSkuId(skuId);
        cache.setFlashSaleId(flashSale.getId());
        cache.setCartId(cartId);
        cache.setCartNum(cartNum);
        redisTemplate.opsForValue().set(buildFlashReserveKey(orderNo), JSONUtil.toJsonStr(cache),
                resolveFlashReserveSeconds(flashSale), TimeUnit.SECONDS);
    }

    private void initializeOrderPaymentCountdown(ShopOrder order) {
        if (order == null || order.getOrderId() == null || order.getOrderId().isBlank()) {
            return;
        }
        redisTemplate.opsForValue().set(buildOrderPaymentKey(order.getOrderId()), order.getOrderId(),
                getPaymentTimeoutSeconds(), TimeUnit.SECONDS);
    }

    private long ensureOrderPaymentRemainingSeconds(ShopOrder order) {
        if (order == null) {
            return 0L;
        }
        int paid = normalizePaidStatus(order.getPaid());
        if (paid == ORDER_PAID_SUCCESS || paid == ORDER_PAID_FAILED) {
            clearOrderPaymentCountdown(order.getOrderId());
            return 0L;
        }

        String key = buildOrderPaymentKey(order.getOrderId());
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            return ttl;
        }

        LocalDateTime startTime = order.getUpdateTime() != null ? order.getUpdateTime() : order.getCreateTime();
        if (startTime == null) {
            initializeOrderPaymentCountdown(order);
            return getPaymentTimeoutSeconds();
        }

        long remainingSeconds = getPaymentTimeoutSeconds() - Math.max(Duration.between(startTime, LocalDateTime.now()).getSeconds(), 0L);
        if (remainingSeconds > 0) {
            redisTemplate.opsForValue().set(key, order.getOrderId(), remainingSeconds, TimeUnit.SECONDS);
            return remainingSeconds;
        }

        expireOrder(order);
        return 0L;
    }

    private void expireOrder(ShopOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        if (normalizePaidStatus(order.getPaid()) == ORDER_PAID_FAILED) {
            clearOrderPaymentCountdown(order.getOrderId());
            return;
        }

        int affectRows = mapper.expirePendingOrder(order.getId(), ORDER_PAID_FAILED,
                ShopOrderStatusEnum.CANCELLED.getCode(), ORDER_PAID_PENDING);
        if (affectRows > 0) {
            order.setPaid(ORDER_PAID_FAILED);
            order.setStatus(ShopOrderStatusEnum.CANCELLED.getCode());
            if (resolveOrderType(order) == ORDER_TYPE_FLASH) {
                releaseFlashReservedStock(order, ShopOrderStatusConstant.CHANGE_MESSAGE_FLASH_RELEASE);
            } else {
                addOrderStatusLog(order, ShopOrderStatusEnum.CANCELLED.getCode());
            }
        }
        clearOrderPaymentCountdown(order.getOrderId());
    }

    private void clearOrderPaymentCountdown(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        redisTemplate.delete(buildOrderPaymentKey(orderNo));
    }

    private long refreshFlashReserveState(ShopOrder order) {
        if (order == null || order.getId() == null || resolveOrderType(order) != ORDER_TYPE_FLASH) {
            return 0L;
        }
        if (normalizePaidStatus(order.getPaid()) != ORDER_PAID_PENDING) {
            return 0L;
        }

        String reserveKey = buildFlashReserveKey(order.getOrderId());
        Long reserveTtl = redisTemplate.getExpire(reserveKey, TimeUnit.SECONDS);
        if (reserveTtl != null && reserveTtl > 0) {
            return reserveTtl;
        }

        Long paymentTtl = redisTemplate.getExpire(buildOrderPaymentKey(order.getOrderId()), TimeUnit.SECONDS);
        if (paymentTtl != null && paymentTtl > 0) {
            return paymentTtl;
        }

        int affectRows = mapper.expirePendingOrder(order.getId(), ORDER_PAID_FAILED,
                ShopOrderStatusEnum.CANCELLED.getCode(), ORDER_PAID_PENDING);
        if (affectRows > 0) {
            order.setPaid(ORDER_PAID_FAILED);
            order.setStatus(ShopOrderStatusEnum.CANCELLED.getCode());
            releaseFlashReservedStock(order, ShopOrderStatusConstant.CHANGE_MESSAGE_FLASH_RELEASE);
        }
        redisTemplate.delete(reserveKey);
        return 0L;
    }

    private void consumeFlashReserve(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        redisTemplate.delete(buildFlashReserveKey(orderNo));
    }

    private long resolveFlashReserveSeconds(ShopFlashSale flashSale) {
        if (flashSale == null || flashSale.getReserveSeconds() == null || flashSale.getReserveSeconds() <= 0) {
            return 900L;
        }
        return flashSale.getReserveSeconds();
    }

    private String buildFlashReserveKey(String orderNo) {
        return FLASH_RESERVE_KEY_PREFIX + orderNo;
    }

    private String buildFlashReserveLockKey(Long flashSaleId, Long skuId) {
        return FLASH_RESERVE_KEY_PREFIX + "lock:" + flashSaleId + ":" + skuId;
    }

    private void assertOrderPending(ShopOrder order) {
        int paid = normalizePaidStatus(order == null ? null : order.getPaid());
        CommonResponseEnum.INVALID.message("订单已支付").assertTrue(paid == ORDER_PAID_SUCCESS);
        CommonResponseEnum.INVALID.message("订单已失效，请重新下单").assertTrue(paid == ORDER_PAID_FAILED);
    }

    private int normalizePaidStatus(Integer paid) {
        if (paid == null) {
            return ORDER_PAID_PENDING;
        }
        if (paid == ORDER_PAID_SUCCESS || paid == ORDER_PAID_FAILED) {
            return paid;
        }
        return ORDER_PAID_PENDING;
    }

    private long getPaymentTimeoutSeconds() {
        long timeoutMinutes = shopPaymentProperties.getTimeoutMinutes();
        if (timeoutMinutes <= 0) {
            timeoutMinutes = 30L;
        }
        return TimeUnit.MINUTES.toSeconds(timeoutMinutes);
    }

    private String buildOrderPaymentKey(String orderNo) {
        return ORDER_PAYMENT_TTL_KEY_PREFIX + orderNo;
    }

    private long refreshFrontOrderPaymentState(ShopOrder order) {
        if (order == null || normalizePaidStatus(order.getPaid()) != ORDER_PAID_PENDING) {
            return 0L;
        }
        return ensureOrderPaymentRemainingSeconds(order);
    }

    private List<ShopProductCartAndSkuVO> fetchOrderCartRows(List<Long> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ShopProductCartAndSkuVO> cartRows = mapper.selectOrderCartWithSku(cartIds, null, null);
        sortCartRowsByIds(cartRows, cartIds);
        return cartRows;
    }

    private void sortCartRowsByIds(List<ShopProductCartAndSkuVO> cartRows, List<Long> orderedIds) {
        Map<Long, Integer> cartIdOrder = new HashMap<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            cartIdOrder.put(orderedIds.get(i), i);
        }
        cartRows.sort(Comparator.comparingInt(row -> cartIdOrder.getOrDefault(row.getId(), Integer.MAX_VALUE)));
    }

    private List<ShopOrderStatusVO> loadOrderStatusTimeline(Long orderId, boolean descending) {
        ShopOrderStatusDTO.ListQuery query = new ShopOrderStatusDTO.ListQuery();
        query.setOid(orderId);
        List<ShopOrderStatusVO> list = shopOrderStatusService.list(query);
        if (list == null) {
            return Collections.emptyList();
        }
        Comparator<ShopOrderStatusVO> cmp = Comparator.comparing(
                ShopOrderStatusVO::getCreateTime, Comparator.nullsLast(LocalDateTime::compareTo));
        list.sort(descending ? cmp.reversed() : cmp);
        return list;
    }

    private PageResult<ShopFrontOrderVO> sliceFrontOrderPage(List<ShopFrontOrderVO> rows, Integer pageNum, Integer limitNum) {
        int page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int limit = limitNum == null || limitNum < 1 ? 10 : limitNum;
        int total = rows.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(fromIndex + limit, total);
        long totalPage = total == 0 ? 0 : (total + limit - 1L) / limit;
        return new PageResult<>(page, limit, totalPage, total, rows.subList(fromIndex, toIndex));
    }

    private ShopOrderAfterSale findOrderAfterSale(Long orderId) {
        if (orderId == null) {
            return null;
        }
        return shopOrderAfterSaleMapper.selectActiveByOrderId(orderId);
    }

    private ShopFrontOrderProductVO toFrontOrderProduct(ShopProductCartAndSkuVO cartRow) {
        ShopFrontOrderProductVO item = new ShopFrontOrderProductVO();
        item.setProductId(cartRow.getProductId());
        item.setSkuId(cartRow.getSkuId());
        item.setProductName(cartRow.getProductName());
        item.setImage(cartRow.getImage());
        item.setSku(cartRow.getSku());
        item.setPrice(cartRow.getPrice() == null ? "0" : cartRow.getPrice().toPlainString());
        item.setCartNum(cartRow.getCartNum());
        return item;
    }

    private static BigDecimal calcMatchedAmountForOrder(ShopProductCoupon coupon,
                                                        Set<Long> orderProductIds,
                                                        Map<Long, BigDecimal> productAmount) {
        if (coupon == null || orderProductIds == null || orderProductIds.isEmpty()) {
            return null;
        }
        String cateKey = coupon.getCateKey();
        List<String> cateTokens = cateKey == null ? Collections.emptyList() : Arrays.stream(cateKey.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        boolean isAllProducts = cateTokens.isEmpty() || cateTokens.contains("0");

        Set<Long> hitProductIds;
        if (isAllProducts) {
            hitProductIds = orderProductIds;
        } else {
            Set<Long> couponProductIds = cateTokens.stream().map(Long::valueOf).collect(Collectors.toSet());
            hitProductIds = orderProductIds.stream().filter(couponProductIds::contains).collect(Collectors.toSet());
        }
        if (hitProductIds.isEmpty()) {
            return null;
        }
        return hitProductIds.stream()
                .map(pid -> productAmount.getOrDefault(pid, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String resolvePayType(ShopOrderDTO.Payment dto) {
        String payType = dto.getPayType();
        if (payType == null || payType.isBlank()) {
            payType = dto.getPayChannel();
        }
        return payType == null ? null : payType.trim().toLowerCase(Locale.ROOT);
    }

    private static BigDecimal resolvePayPrice(ShopOrder order) {
        if (order.getPayPrice() != null) {
            return order.getPayPrice();
        }
        BigDecimal totalPrice = order.getTotalPrice() == null ? BigDecimal.ZERO : order.getTotalPrice();
        BigDecimal freightPrice = order.getFreightPrice() == null ? BigDecimal.ZERO : order.getFreightPrice();
        BigDecimal discount = order.getDeductionPrice() != null ? order.getDeductionPrice() : order.getCouponPrice();
        if (discount == null) {
            discount = BigDecimal.ZERO;
        }
        return totalPrice.add(freightPrice).subtract(discount).max(BigDecimal.ZERO);
    }

    private int resolveOrderType(ShopOrder order) {
        if (order == null) {
            return ORDER_TYPE_NORMAL;
        }
        return order.getSeckillId() != null && order.getSeckillId() > 0 ? ORDER_TYPE_FLASH : ORDER_TYPE_NORMAL;
    }

    private String resolveOrderTypeLabel(ShopOrder order) {
        return resolveOrderType(order) == ORDER_TYPE_FLASH ? "秒杀订单" : "普通订单";
    }

    private static List<Long> parseOrderCartIds(String cartIdsRaw) {
        CommonResponseEnum.INVALID.message("订单购物车数据无效")
                .assertTrue(cartIdsRaw == null || cartIdsRaw.isBlank());
        List<String> tokens = Arrays.stream(cartIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        CommonResponseEnum.INVALID.message("订单购物车数据无效").assertTrue(tokens.isEmpty());
        CommonResponseEnum.INVALID.message("订单购物车数据无效")
                .assertTrue(tokens.stream().anyMatch(s -> !s.matches("\\d+")));

        List<Long> cartIds = tokens.stream().map(Long::valueOf).distinct().collect(Collectors.toList());
        CommonResponseEnum.INVALID.message("订单购物车数据无效").assertTrue(cartIds.isEmpty());
        return cartIds;
    }

    private static List<Long> parseOrderCartIdsQuietly(String cartIdsRaw) {
        if (cartIdsRaw == null || cartIdsRaw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(cartIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> s.matches("\\d+"))
                .map(Long::valueOf)
                .distinct()
                .collect(Collectors.toList());
    }

    private static boolean matchesFrontOrderTab(ShopFrontOrderVO order, String tab) {
        if (tab == null || tab.isBlank() || "all".equalsIgnoreCase(tab.trim())) {
            return true;
        }
        return tab.trim().equalsIgnoreCase(order.getDisplayTab());
    }

    private static boolean matchesFrontOrderKeyword(ShopFrontOrderVO order, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        if (order.getOrderNo() != null && order.getOrderNo().toLowerCase(Locale.ROOT).contains(normalized)) {
            return true;
        }
        return order.getOrderInfoList() != null && order.getOrderInfoList().stream()
                .map(ShopFrontOrderProductVO::getProductName)
                .filter(Objects::nonNull)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(name -> name.contains(normalized));
    }

    private String resolveFrontDisplayTab(ShopOrder order) {
        int paid = normalizePaidStatus(order.getPaid());
        int status = order.getStatus() == null ? 0 : order.getStatus();
        if (paid == ORDER_PAID_PENDING) {
            return "pending_payment";
        }
        if (paid == ORDER_PAID_FAILED || status == ShopOrderStatusEnum.CANCELLED.getCode()) {
            return "cancelled";
        }
        if (paid == ORDER_PAID_SUCCESS && status == 0) {
            return "pending_shipment";
        }
        if (paid == ORDER_PAID_SUCCESS && status == 1) {
            return "pending_receipt";
        }
        if (paid == ORDER_PAID_SUCCESS && status == 2) {
            return "pending_review";
        }
        return "completed";
    }

    private String resolveFrontDisplayStatus(ShopOrder order, ShopOrderAfterSale afterSale) {
        String afterSaleStatusText = resolveAfterSaleStatusText(afterSale);
        if (afterSaleStatusText != null && !afterSaleStatusText.isBlank()) {
            return afterSaleStatusText;
        }
        return switch (resolveFrontDisplayTab(order)) {
            case "pending_payment" -> "待付款";
            case "pending_shipment" -> "待发货";
            case "pending_receipt" -> "待收货";
            case "pending_review" -> "待评价";
            case "cancelled" -> "已取消";
            default -> "已完成";
        };
    }

    private boolean canApplyAfterSale(ShopOrder order, ShopOrderAfterSale afterSale) {
        if (order == null || normalizePaidStatus(order.getPaid()) != ORDER_PAID_SUCCESS) {
            return false;
        }
        int status = order.getStatus() == null ? -1 : order.getStatus();
        return (status == 2 || status == 3) && afterSale == null;
    }

    private boolean hasBlockingAfterSale(ShopOrderAfterSale afterSale) {
        return afterSale != null && afterSale.getStatus() != null && afterSale.getStatus() != 4;
    }

    private String resolveAfterSaleStatusText(ShopOrderAfterSale afterSale) {
        if (afterSale == null || afterSale.getStatus() == null) {
            return null;
        }
        return switch (afterSale.getStatus()) {
            case 0 -> "售后待审核";
            case 1 -> "待寄回商品";
            case 2 -> "退货待退款";
            case 3 -> "已退款";
            case 4 -> "售后已拒绝";
            default -> null;
        };
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

    private void addOrderStatusLog(ShopOrder order, Integer newStatus) {
        if (newStatus == null) {
            return;
        }
        String changeType;
        String changeMessage;
        switch (newStatus) {
            case 0 -> {
                changeType = ShopOrderStatusConstant.CHANGE_TYPE_CREATE_ORDER;
                changeMessage = ShopOrderStatusConstant.CHANGE_MESSAGE_CREATE_ORDER;
            }
            case 1 -> {
                changeType = ShopOrderStatusConstant.CHANGE_TYPE_PAY_SUCCESS;
                changeMessage = ShopOrderStatusConstant.CHANGE_MESSAGE_PAY_SUCCESS;
            }
            case 2 -> {
                changeType = ShopOrderStatusConstant.CHANGE_TYPE_DELIVERY;
                changeMessage = String.format(ShopOrderStatusConstant.CHANGE_MESSAGE_DELIVERY_TEMPLATE,
                        resolveExpressName(order.getDeliveryId()));
            }
            case 3 -> {
                changeType = ShopOrderStatusConstant.CHANGE_TYPE_USER_TAKE_DELIVERY;
                changeMessage = ShopOrderStatusConstant.CHANGE_MESSAGE_USER_TAKE_DELIVERY;
            }
            case 4 -> {
                changeType = ShopOrderStatusConstant.CHANGE_TYPE_ORDER_EVAL;
                changeMessage = ShopOrderStatusConstant.CHANGE_MESSAGE_ORDER_EVAL;
            }
            case 5 -> {
                changeType = ShopOrderStatusConstant.CHANGE_TYPE_CANCEL_ORDER;
                changeMessage = ShopOrderStatusConstant.CHANGE_MESSAGE_CANCEL_ORDER;
            }
            case 6 -> {
                changeType = ShopOrderStatusConstant.CHANGE_TYPE_REFUNDING;
                changeMessage = ShopOrderStatusConstant.CHANGE_MESSAGE_REFUNDING;
            }
            case 7 -> {
                changeType = ShopOrderStatusConstant.CHANGE_TYPE_REFUNDED;
                changeMessage = ShopOrderStatusConstant.CHANGE_MESSAGE_REFUNDED;
            }
            case 8 -> {
                changeType = ShopOrderStatusConstant.CHANGE_TYPE_CLOSED;
                changeMessage = ShopOrderStatusConstant.CHANGE_MESSAGE_CLOSED;
            }
            default -> {
                return;
            }
        }
        saveOrderStatusLog(order.getId(), changeType, changeMessage);
    }

    private String resolveExpressName(Long deliveryId) {
        if (deliveryId == null) {
            return "UNKNOWN";
        }
        ShopExpress express = shopExpressService.getById(deliveryId);
        if (express == null || express.getExpressName() == null || express.getExpressName().isBlank()) {
            return "UNKNOWN";
        }
        return express.getExpressName();
    }

    private static String joinAddress(ShopUserAddress address) {
        if (address == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (address.getProvince() != null) sb.append(address.getProvince());
        if (address.getCity() != null) sb.append(address.getCity());
        if (address.getDistrict() != null) sb.append(address.getDistrict());
        if (address.getDetail() != null) sb.append(address.getDetail());
        String full = sb.toString().trim();
        return full.isBlank() ? null : full;
    }

    private static String joinAddressWithSpaces(ShopUserAddress address) {
        if (address == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (address.getProvince() != null && !address.getProvince().isBlank()) parts.add(address.getProvince().trim());
        if (address.getCity() != null && !address.getCity().isBlank()) parts.add(address.getCity().trim());
        if (address.getDistrict() != null && !address.getDistrict().isBlank()) parts.add(address.getDistrict().trim());
        if (address.getDetail() != null && !address.getDetail().isBlank()) parts.add(address.getDetail().trim());
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private void validateDirectCreateRequest(Long userId, AgentMallDTO.DirectCreateOrder dto) {
        CommonResponseEnum.INVALID_TOKEN.assertTrue(userId == null || userId <= 0);
        CommonResponseEnum.INVALID.message("下单参数不能为空").assertNull(dto);
        CommonResponseEnum.INVALID_ID.assertNull(dto.getProductId());
        CommonResponseEnum.INVALID_ID.assertNull(dto.getSkuId());
        CommonResponseEnum.INVALID.message("购买数量必须大于0").assertTrue(dto.getQuantity() == null || dto.getQuantity() <= 0);
        CommonResponseEnum.INVALID.message("收货地址不能为空").assertTrue(dto.getAddressId() == null || dto.getAddressId() <= 0);
        CommonResponseEnum.INVALID.message("幂等键不能为空")
                .assertTrue(dto.getIdempotencyKey() == null || dto.getIdempotencyKey().isBlank());
    }

    private AgentMallVO.DirectOrderResult executeDirectCreateForAgent(Long userId, AgentMallDTO.DirectCreateOrder dto) {
        LocalDateTime now = LocalDateTime.now();
        ShopProduct product = shopProductService.getById(dto.getProductId());
        CommonResponseEnum.INVALID.message("商品不存在或已下架")
                .assertTrue(product == null
                        || (product.getIsDel() != null && product.getIsDel() == 1)
                        || product.getIsShow() == null
                        || product.getIsShow() != 1);

        ShopFlashSale activeFlashSale = shopFlashSaleService.getAvailableByProductId(dto.getProductId(), now);
        CommonResponseEnum.INVALID.message("秒杀活动商品不支持客服直接下单，请前往秒杀页面抢购")
                .assertTrue(activeFlashSale != null);

        ShopProductAttrValue sku = shopProductAttrValueService.getById(dto.getSkuId());
        CommonResponseEnum.INVALID.message("商品规格不存在或已下架")
                .assertTrue(sku == null || (sku.getIsDel() != null && sku.getIsDel() == 1));
        CommonResponseEnum.INVALID.message("商品规格与商品不匹配")
                .assertTrue(sku.getProductId() == null || !dto.getProductId().equals(sku.getProductId()));

        int quantity = dto.getQuantity();
        int skuStock = sku.getStock() == null ? 0 : sku.getStock();
        CommonResponseEnum.INVALID.message("商品规格库存不足").assertTrue(skuStock < quantity);
        if (product.getStock() != null && product.getStock() < quantity) {
            CommonResponseEnum.INVALID.message("商品库存不足").assertTrue(true);
        }

        ShopUserAddress address = requireUserAddress(userId, dto.getAddressId());

        BigDecimal unitPrice = sku.getPrice() != null
                ? sku.getPrice()
                : (product.getPrice() == null ? BigDecimal.ZERO : product.getPrice());
        CommonResponseEnum.INVALID.message("商品价格无效").assertTrue(unitPrice.compareTo(BigDecimal.ZERO) <= 0);
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal freightPrice = resolveProductFreightPrice(product);
        BigDecimal payPrice = totalPrice.add(freightPrice);

        // 创建 status=2 的隐藏购物车快照，以便支付接口统一扣减库存且不污染用户购物车角标
        ShopProductCart snapshotCart = new ShopProductCart();
        snapshotCart.setUid(userId);
        snapshotCart.setProductId(product.getId());
        snapshotCart.setSkuId(sku.getId());
        snapshotCart.setCartNum(quantity);
        snapshotCart.setSeckillId(0);
        snapshotCart.setBargainId(0);
        snapshotCart.setStatus(2);
        shopProductCartMapper.insert(snapshotCart);
        CommonResponseEnum.INVALID.message("创建订单商品快照失败").assertNull(snapshotCart.getId());

        String orderNo = SerialNumberGenerator.generateOrderNo();
        ShopOrder order = new ShopOrder()
                .setOrderId(orderNo)
                .setUid(userId)
                .setRealName(address.getRealName())
                .setUserPhone(address.getPhone())
                .setUserAddress(joinAddressWithSpaces(address))
                .setCartIds(String.valueOf(snapshotCart.getId()))
                .setTotalNum(quantity)
                .setTotalPrice(totalPrice)
                .setFreightPrice(freightPrice)
                .setPayPrice(payPrice)
                .setCouponPrice(BigDecimal.ZERO)
                .setDeductionPrice(BigDecimal.ZERO)
                .setUseIntegral(0)
                .setPaid(ORDER_PAID_PENDING)
                .setStatus(ShopOrderStatusEnum.CREATED.getCode())
                .setSeckillId(0)
                .setPinkId(0)
                .setBargainId(0)
                .setIsDel(0);
        save(order);
        saveOrderStatusLog(order.getId(), ShopOrderStatusConstant.CHANGE_TYPE_CREATE_ORDER, ShopOrderStatusConstant.CHANGE_MESSAGE_CREATE_ORDER);
        initializeOrderPaymentCountdown(order);

        AgentMallVO.DirectOrderResult result = new AgentMallVO.DirectOrderResult();
        result.setOrderNo(orderNo);
        result.setTotalPrice(totalPrice);
        result.setFreightPrice(freightPrice);
        result.setPayPrice(payPrice);
        result.setRemainingSeconds(getPaymentTimeoutSeconds());
        result.setPaymentUrl("/order_payment?orderNo=" + orderNo);
        result.setPaid(ORDER_PAID_PENDING);
        result.setStatus(ShopOrderStatusEnum.CREATED.getCode());
        return result;
    }

    private static BigDecimal resolveProductFreightPrice(ShopProduct product) {
        if (product.getIsPostage() != null && product.getIsPostage() == 1) {
            return BigDecimal.ZERO;
        }
        if (product.getPostage() == null || product.getPostage().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return product.getPostage();
    }

    private String buildAgentOrderIdempKey(Long userId, String idempotencyKey) {
        return AGENT_ORDER_IDEMP_KEY_PREFIX + userId + ":" + idempotencyKey.trim();
    }

    private AgentMallVO.DirectOrderResult readAgentIdempotentOrder(String idempKey) {
        Object cached = redisTemplate.opsForValue().get(idempKey);
        if (cached == null) {
            return null;
        }
        if (cached instanceof AgentMallVO.DirectOrderResult result) {
            return result;
        }
        String raw = String.valueOf(cached).trim();
        if (raw.isEmpty()) {
            return null;
        }
        return JSONUtil.toBean(raw, AgentMallVO.DirectOrderResult.class);
    }

    @Data
    private static class CartSummary {
        private int totalNum = 0;
        private BigDecimal totalPrice = BigDecimal.ZERO;
        private BigDecimal totalPostage = BigDecimal.ZERO;
        private Map<Long, String> productNames = new HashMap<>();
        private Map<Long, BigDecimal> productAmount = new HashMap<>();
        private Map<Long, Integer> productCateIds = new HashMap<>();
    }

    @Data
    private static class CouponChoice {
        private ShopCouponAndUserVO selectedCoupon;
        private BigDecimal couponPrice = BigDecimal.ZERO;
    }

    @Data
    private static class CouponConfirmResult {
        private ShopProductCoupon coupon;
        private Long usedCouponUserId;
        private BigDecimal couponDiscount = BigDecimal.ZERO;
    }

    @Data
    private static class IntegralDeduction {
        private long useIntegral = 0L;
        private BigDecimal deductionPrice = BigDecimal.ZERO;
    }

    @Data
    private static class FlashReserveCache {
        private String orderNo;
        private Long orderId;
        private Long productId;
        private Long skuId;
        private Long flashSaleId;
        private Long cartId;
        private Integer cartNum;
    }
}
