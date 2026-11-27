package com.sz.admin.shop_system.agent;

import com.sz.admin.shop_system.agent.pojo.dto.AgentMallDTO;
import com.sz.admin.shop_system.agent.pojo.vo.AgentMallVO;
import com.sz.admin.shop_system.agent.service.impl.AgentMallServiceImpl;
import com.sz.admin.shop_system.constants.ShopOrderStatusConstant;
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
import com.sz.admin.shop_system.pojo.po.ShopUserAddress;
import com.sz.admin.shop_system.pojo.vo.ShopFrontOrderVO;
import com.sz.admin.shop_system.pojo.vo.ShopOrderStatusVO;
import com.sz.admin.shop_system.pojo.vo.ShopProductCartAndSkuVO;
import com.sz.core.common.entity.PageResult;
import com.sz.admin.shop_system.properties.ShopPaymentProperties;
import com.sz.admin.shop_system.service.ShopCategoryService;
import com.sz.admin.shop_system.service.ShopExpressService;
import com.sz.admin.shop_system.service.ShopFlashSaleService;
import com.sz.admin.shop_system.service.ShopOrderStatusService;
import com.sz.admin.shop_system.service.ShopProductAttrValueService;
import com.sz.admin.shop_system.service.ShopProductService;
import com.sz.admin.shop_system.service.ShopUserBillService;
import com.sz.admin.shop_system.service.ShopUserIntegralBillService;
import com.sz.admin.shop_system.service.ShopUserService;
import com.sz.admin.shop_system.service.impl.ShopOrderServiceImpl;
import com.sz.core.common.exception.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentMallServiceImplTest {

    @Mock
    private ShopUserService shopUserService;
    @Mock
    private ShopProductCartMapper shopProductCartMapper;
    @Mock
    private ShopOrderStatusService shopOrderStatusService;
    @Mock
    private ShopProductAttrValueService shopProductAttrValueService;
    @Mock
    private ShopProductService shopProductService;
    @Mock
    private ShopProductMapper shopProductMapper;
    @Mock
    private ShopProductAttrValueMapper shopProductAttrValueMapper;
    @Mock
    private ShopUserBillService shopUserBillService;
    @Mock
    private ShopUserIntegralBillService shopUserIntegralBillService;
    @Mock
    private ShopExpressService shopExpressService;
    @Mock
    private ShopExpressInfoMapper shopExpressInfoMapper;
    @Mock
    private ShopExpressTemplateMapper shopExpressTemplateMapper;
    @Mock
    private ShopUserAddressMapper shopUserAddressMapper;
    @Mock
    private ShopProductCouponMapper shopProductCouponMapper;
    @Mock
    private ShopProductCouponUserMapper shopProductCouponUserMapper;
    @Mock
    private ShopProductCommentMapper shopProductCommentMapper;
    @Mock
    private ShopOrderAfterSaleMapper shopOrderAfterSaleMapper;
    @Mock
    private ShopOrderMapper shopOrderMapper;
    @Mock
    private RedisTemplate<Object, Object> redisTemplate;
    @Mock
    private ValueOperations<Object, Object> valueOperations;
    @Mock
    private ShopPaymentProperties shopPaymentProperties;
    @Mock
    private ShopFlashSaleService shopFlashSaleService;
    @Mock
    private ShopCategoryService shopCategoryService;
    @Mock
    private TransactionTemplate transactionTemplate;

    private ShopOrderServiceImpl shopOrderService;
    private AgentMallServiceImpl agentMallService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        shopOrderService = new ShopOrderServiceImpl(
                shopUserService,
                shopProductCartMapper,
                shopOrderStatusService,
                shopProductAttrValueService,
                shopProductService,
                shopProductMapper,
                shopProductAttrValueMapper,
                shopUserBillService,
                shopUserIntegralBillService,
                shopExpressService,
                shopExpressInfoMapper,
                shopExpressTemplateMapper,
                shopUserAddressMapper,
                shopProductCouponMapper,
                shopProductCouponUserMapper,
                shopProductCommentMapper,
                shopOrderAfterSaleMapper,
                redisTemplate,
                shopPaymentProperties,
                shopFlashSaleService,
                transactionTemplate
        );
        ReflectionTestUtils.setField(shopOrderService, "mapper", shopOrderMapper);

        agentMallService = new AgentMallServiceImpl(
                shopOrderService,
                shopOrderMapper,
                shopProductMapper,
                shopProductService,
                shopProductAttrValueService,
                shopFlashSaleService,
                shopCategoryService,
                shopUserAddressMapper,
                shopExpressInfoMapper,
                shopExpressTemplateMapper,
                shopExpressService,
                shopOrderAfterSaleMapper
        );
    }

    @Test
    void directCreate_rejectsOtherUsersAddress() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("shop:agent:order:idemp:101:draft-idor-1")).thenReturn(null);
        when(valueOperations.setIfAbsent(eq("shop:agent:order:idemp:101:draft-idor-1:lock"), anyString(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        ShopProduct product = buildActiveProduct(10L, 1, BigDecimal.ZERO);
        when(shopProductService.getById(10L)).thenReturn(product);
        when(shopFlashSaleService.getAvailableByProductId(eq(10L), any(LocalDateTime.class))).thenReturn(null);
        when(shopProductAttrValueService.getById(100L)).thenReturn(buildActiveSku(100L, 10L, new BigDecimal("399.00"), 10));
        when(shopUserAddressMapper.selectActiveByIdAndUid(999L, 101L)).thenReturn(null);

        AgentMallDTO.DirectCreateOrder dto = buildDirectOrderRequest(10L, 100L, 1, 999L, "draft-idor-1");

        BusinessException ex = assertThrows(BusinessException.class, () -> agentMallService.directCreateOrder(101L, dto));
        assertTrue(ex.getMessage().contains("收货地址"));
        verify(shopOrderMapper, never()).insert(any(ShopOrder.class), anyBoolean());
        verify(shopProductCartMapper, never()).insert(any(ShopProductCart.class));
        verify(redisTemplate, never()).delete(any(Object.class));
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("shop:agent:order:idemp:101:draft-idor-1:lock")), anyString());
    }

    @Test
    void directCreate_rejectsActiveFlashSaleProduct() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("shop:agent:order:idemp:101:draft-flash-1")).thenReturn(null);
        when(valueOperations.setIfAbsent(eq("shop:agent:order:idemp:101:draft-flash-1:lock"), anyString(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        ShopProduct product = buildActiveProduct(20L, 1, BigDecimal.ZERO);
        when(shopProductService.getById(20L)).thenReturn(product);
        ShopFlashSale flashSale = new ShopFlashSale();
        flashSale.setId(8L);
        flashSale.setProductId(20L);
        when(shopFlashSaleService.getAvailableByProductId(eq(20L), any(LocalDateTime.class))).thenReturn(flashSale);

        AgentMallDTO.DirectCreateOrder dto = buildDirectOrderRequest(20L, 200L, 1, 55L, "draft-flash-1");

        BusinessException ex = assertThrows(BusinessException.class, () -> agentMallService.directCreateOrder(101L, dto));
        assertTrue(ex.getMessage().contains("秒杀"));
        verify(shopOrderMapper, never()).insert(any(ShopOrder.class), anyBoolean());
        verify(shopProductAttrValueService, never()).getById(any());
        verify(redisTemplate, never()).delete(any(Object.class));
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("shop:agent:order:idemp:101:draft-flash-1:lock")), anyString());
    }

    @Test
    void directCreate_isIdempotentAndDoesNotDeductStock() {
        Map<Object, Object> redisStore = new ConcurrentHashMap<>();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenAnswer(inv -> redisStore.get(inv.getArgument(0)));
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any(TimeUnit.class))).thenAnswer(inv -> {
            Object key = inv.getArgument(0);
            Object val = inv.getArgument(1);
            return redisStore.putIfAbsent(key, val) == null;
        });
        doAnswer(inv -> {
            redisStore.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOperations).set(any(), any(), anyLong(), any(TimeUnit.class));
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenAnswer(inv -> {
            List<?> keys = inv.getArgument(1);
            Object lockKey = keys.get(0);
            Object expectedToken = inv.getArgument(2);
            if (expectedToken != null && expectedToken.equals(redisStore.get(lockKey))) {
                redisStore.remove(lockKey);
                return 1L;
            }
            return 0L;
        });

        ShopProduct product = buildActiveProduct(10L, 0, new BigDecimal("12.00"));
        when(shopProductService.getById(10L)).thenReturn(product);
        when(shopFlashSaleService.getAvailableByProductId(eq(10L), any(LocalDateTime.class))).thenReturn(null);
        when(shopProductAttrValueService.getById(100L)).thenReturn(buildActiveSku(100L, 10L, new BigDecimal("399.00"), 5));

        ShopUserAddress address = new ShopUserAddress();
        address.setId(55L);
        address.setUid(101L);
        address.setRealName("张三");
        address.setPhone("13800138000");
        address.setProvince("上海市");
        address.setCity("上海市");
        address.setDistrict("浦东新区");
        address.setDetail("张江高科88号");
        when(shopUserAddressMapper.selectActiveByIdAndUid(55L, 101L)).thenReturn(address);

        when(shopProductCartMapper.insert(any(ShopProductCart.class))).thenAnswer(inv -> {
            ShopProductCart cart = inv.getArgument(0);
            cart.setId(501L);
            return 1;
        });
        when(shopOrderMapper.insert(any(ShopOrder.class), eq(true))).thenAnswer(inv -> {
            ShopOrder order = inv.getArgument(0);
            order.setId(9001L);
            return 1;
        });
        when(shopPaymentProperties.getTimeoutMinutes()).thenReturn(30L);

        AgentMallDTO.DirectCreateOrder dto = buildDirectOrderRequest(10L, 100L, 2, 55L, "draft-abc");

        AgentMallVO.DirectOrderResult first = agentMallService.directCreateOrder(101L, dto);
        AgentMallVO.DirectOrderResult second = agentMallService.directCreateOrder(101L, dto);

        assertNotNull(first.getOrderNo());
        assertEquals(first.getOrderNo(), second.getOrderNo());
        assertEquals("/order_payment?orderNo=" + first.getOrderNo(), first.getPaymentUrl());
        assertEquals(first.getPaymentUrl(), second.getPaymentUrl());
        assertEquals(0, new BigDecimal("810.00").compareTo(first.getPayPrice()));
        assertEquals(0, new BigDecimal("12.00").compareTo(first.getFreightPrice()));
        assertEquals(1800L, first.getRemainingSeconds());
        assertEquals(0, first.getPaid());
        assertEquals(0, first.getStatus());

        ArgumentCaptor<ShopOrder> orderCaptor = ArgumentCaptor.forClass(ShopOrder.class);
        verify(shopOrderMapper, times(1)).insert(orderCaptor.capture(), eq(true));
        ShopOrder savedOrder = orderCaptor.getValue();
        assertEquals(101L, savedOrder.getUid());
        assertEquals(0, savedOrder.getPaid());
        assertEquals(0, savedOrder.getStatus());
        assertEquals(0, savedOrder.getSeckillId());
        assertEquals("501", savedOrder.getCartIds());
        assertEquals(2, savedOrder.getTotalNum());
        assertEquals(0, new BigDecimal("798.00").compareTo(savedOrder.getTotalPrice()));
        assertEquals(0, new BigDecimal("12.00").compareTo(savedOrder.getFreightPrice()));
        assertEquals(0, new BigDecimal("810.00").compareTo(savedOrder.getPayPrice()));

        verify(shopProductCartMapper, times(1)).insert(any(ShopProductCart.class));

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unlockTokenCaptor = ArgumentCaptor.forClass(String.class);
        InOrder inOrder = inOrder(transactionTemplate, valueOperations, redisTemplate);
        inOrder.verify(valueOperations).setIfAbsent(
                eq("shop:agent:order:idemp:101:draft-abc:lock"),
                tokenCaptor.capture(),
                eq(30L),
                eq(TimeUnit.SECONDS)
        );
        inOrder.verify(transactionTemplate).execute(any());
        inOrder.verify(valueOperations).set(eq("shop:order:payment:ttl:" + first.getOrderNo()), eq(first.getOrderNo()), eq(1800L), eq(TimeUnit.SECONDS));
        inOrder.verify(valueOperations).set(eq("shop:agent:order:idemp:101:draft-abc"), anyString(), eq(24L), eq(TimeUnit.HOURS));
        inOrder.verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("shop:agent:order:idemp:101:draft-abc:lock")), unlockTokenCaptor.capture());

        assertNotEquals("101", tokenCaptor.getValue());
        assertEquals(tokenCaptor.getValue(), unlockTokenCaptor.getValue());
        verify(redisTemplate, never()).delete(any(Object.class));

        ArgumentCaptor<ShopOrderStatus> statusCaptor = ArgumentCaptor.forClass(ShopOrderStatus.class);
        verify(shopOrderStatusService, times(1)).save(statusCaptor.capture());
        assertEquals(9001L, statusCaptor.getValue().getOid());
        assertEquals(ShopOrderStatusConstant.CHANGE_TYPE_CREATE_ORDER, statusCaptor.getValue().getChangeType());

        verify(shopProductAttrValueService, never()).updateById(any());
        verify(shopProductService, never()).updateById(any());
        verifyNoInteractions(shopProductAttrValueMapper);

        AgentMallDTO.DirectCreateOrder differentPayload = buildDirectOrderRequest(10L, 100L, 3, 55L, "draft-abc");
        BusinessException mismatchEx = assertThrows(BusinessException.class,
                () -> agentMallService.directCreateOrder(101L, differentPayload));
        assertTrue(mismatchEx.getMessage().contains("幂等键"));
    }

    @Test
    void directCreate_doesNotWriteRedisCacheWhenTransactionRollsBack() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("shop:agent:order:idemp:101:draft-rollback-1")).thenReturn(null);
        when(valueOperations.setIfAbsent(eq("shop:agent:order:idemp:101:draft-rollback-1:lock"), anyString(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        doThrow(new IllegalStateException("db commit failed")).when(transactionTemplate).execute(any());

        AgentMallDTO.DirectCreateOrder dto = buildDirectOrderRequest(10L, 100L, 1, 55L, "draft-rollback-1");

        assertThrows(IllegalStateException.class, () -> agentMallService.directCreateOrder(101L, dto));
        verify(valueOperations, never()).set(any(), any(), anyLong(), any(TimeUnit.class));
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("shop:agent:order:idemp:101:draft-rollback-1:lock")), anyString());
    }

    @Test
    void getOrderDetail_returnsShipmentRecordWithoutFakeTrackingNodes() {
        String orderNo = "ORDER202611260001";
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 25, 10, 0, 0);
        LocalDateTime paidAt = LocalDateTime.of(2026, 11, 25, 10, 5, 0);
        LocalDateTime shippedAt = LocalDateTime.of(2026, 11, 25, 16, 30, 0);

        ShopOrder order = new ShopOrder()
                .setId(700L)
                .setOrderId(orderNo)
                .setUid(101L)
                .setRealName("张三")
                .setUserPhone("13800138000")
                .setUserAddress("上海市 上海市 浦东新区 张江高科88号")
                .setCartIds("501")
                .setTotalNum(1)
                .setTotalPrice(new BigDecimal("399.00"))
                .setFreightPrice(BigDecimal.ZERO)
                .setPayPrice(new BigDecimal("399.00"))
                .setPaid(1)
                .setPayTime(paidAt)
                .setPayType("yue")
                .setStatus(1)
                .setDeliveryId(3L)
                .setExpressNo("EXP202611260001")
                .setSeckillId(0)
                .setCreateTime(createdAt);
        when(shopOrderMapper.selectByOrderNoAndUid(orderNo, 101L)).thenReturn(order);

        ShopProductCartAndSkuVO cartRow = new ShopProductCartAndSkuVO();
        cartRow.setId(501L);
        cartRow.setProductId(10L);
        cartRow.setSkuId(100L);
        cartRow.setProductName("咔嗒机械键盘");
        cartRow.setSku("红轴/白");
        cartRow.setPrice(new BigDecimal("399.00"));
        cartRow.setCartNum(1);
        when(shopOrderMapper.selectOrderCartWithSku(eq(List.of(501L)), isNull(), isNull())).thenReturn(new ArrayList<>(List.of(cartRow)));

        ShopOrderStatusVO s1 = new ShopOrderStatusVO();
        s1.setOid(700L);
        s1.setChangeType(ShopOrderStatusConstant.CHANGE_TYPE_CREATE_ORDER);
        s1.setChangeMessage(ShopOrderStatusConstant.CHANGE_MESSAGE_CREATE_ORDER);
        s1.setCreateTime(createdAt);

        ShopOrderStatusVO s2 = new ShopOrderStatusVO();
        s2.setOid(700L);
        s2.setChangeType(ShopOrderStatusConstant.CHANGE_TYPE_DELIVERY);
        s2.setChangeMessage("已发货：快递公司为：顺丰速运");
        s2.setCreateTime(shippedAt);
        when(shopOrderStatusService.list(any(ShopOrderStatusDTO.ListQuery.class))).thenReturn(new ArrayList<>(List.of(s1, s2)));

        ShopExpress express = new ShopExpress();
        express.setId(3L);
        express.setExpressName("顺丰速运");
        when(shopExpressService.getById(3L)).thenReturn(express);

        ShopExpressInfo expressInfo = new ShopExpressInfo();
        expressInfo.setId(88L);
        expressInfo.setOid(700L);
        expressInfo.setExpressTmpId(12L);
        expressInfo.setExpressNo("EXP202611260001");
        expressInfo.setRealName("张三");
        expressInfo.setUserPhone("13800138000");
        expressInfo.setUserAddress("上海市 上海市 浦东新区 张江高科88号");
        expressInfo.setStatus(1);
        expressInfo.setCreateTime(shippedAt);
        when(shopExpressInfoMapper.selectLatestByOid(700L)).thenReturn(expressInfo);

        ShopExpressTemplate template = new ShopExpressTemplate();
        template.setId(12L);
        template.setName("顺丰标准达");
        template.setExpressId(3L);
        when(shopExpressTemplateMapper.selectActiveById(12L)).thenReturn(template);

        when(shopOrderAfterSaleMapper.selectActiveByOrderId(700L)).thenReturn(null);

        AgentMallVO.OrderDetail detail = agentMallService.getOrderDetail(101L, orderNo);

        assertEquals(orderNo, detail.getOrderNo());
        assertEquals(1, detail.getStatus());
        assertEquals("已发货", detail.getStatusDesc());
        assertEquals("待收货", detail.getDisplayStatus());
        assertFalse(Boolean.TRUE.equals(detail.getTrackingSupported()));
        assertNotNull(detail.getShipment());
        assertEquals("EXP202611260001", detail.getShipment().getExpressNo());
        assertEquals("顺丰标准达", detail.getShipment().getExpressTemplateName());
        assertEquals("顺丰速运", detail.getShipment().getExpressCompanyName());
        assertEquals(1, detail.getShipment().getStatus());
        assertEquals(shippedAt, detail.getShipment().getCreateTime());
        assertEquals(2, detail.getOrderStatusList().size());
        assertEquals(ShopOrderStatusConstant.CHANGE_TYPE_DELIVERY, detail.getOrderStatusList().get(0).getChangeType());
    }

    @Test
    void getOrderDetail_rejectsOtherUsersOrder() {
        when(shopOrderMapper.selectByOrderNoAndUid("ORDER202611269999", 101L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> agentMallService.getOrderDetail(101L, "ORDER202611269999"));
        assertTrue(ex.getMessage().contains("订单不存在"));
        verifyNoInteractions(shopExpressInfoMapper, shopOrderAfterSaleMapper);
    }

    @Test
    void listUserAddresses_ordersDefaultAddressFirst() {
        ShopUserAddress nonDefault = new ShopUserAddress();
        nonDefault.setId(1L);
        nonDefault.setUid(101L);
        nonDefault.setRealName("李四");
        nonDefault.setPhone("13900139000");
        nonDefault.setProvince("北京市");
        nonDefault.setCity("北京市");
        nonDefault.setDistrict("朝阳区");
        nonDefault.setDetail("建国路1号");
        nonDefault.setIsDefault(0);

        ShopUserAddress defaultAddr = new ShopUserAddress();
        defaultAddr.setId(2L);
        defaultAddr.setUid(101L);
        defaultAddr.setRealName("张三");
        defaultAddr.setPhone("13800138000");
        defaultAddr.setProvince("上海市");
        defaultAddr.setCity("上海市");
        defaultAddr.setDistrict("浦东新区");
        defaultAddr.setDetail("张江高科88号");
        defaultAddr.setIsDefault(1);

        when(shopUserAddressMapper.selectActiveByUid(101L)).thenReturn(List.of(nonDefault, defaultAddr));

        List<AgentMallVO.UserAddress> addresses = agentMallService.listUserAddresses(101L);
        assertEquals(2, addresses.size());
        assertEquals(2L, addresses.get(0).getId());
        assertEquals(1, addresses.get(0).getIsDefault());
        assertEquals("上海市 上海市 浦东新区 张江高科88号", addresses.get(0).getFullAddress());
    }

    @Test
    void directCreateForAgent_doesNotWrapRedisLockAndCacheInTransactional() throws Exception {
        assertFalse(ShopOrderServiceImpl.class
                .getMethod("directCreateForAgent", Long.class, AgentMallDTO.DirectCreateOrder.class)
                .isAnnotationPresent(Transactional.class));
    }

    @Test
    void getOrderDetail_mapsRealPaidStatusAndRefundStatesAccurately() {
        String pendingShipNo = "ORDER_PENDING_SHIP";
        ShopOrder pendingShipOrder = new ShopOrder()
                .setId(701L)
                .setOrderId(pendingShipNo)
                .setUid(101L)
                .setPaid(1)
                .setStatus(0)
                .setCartIds("")
                .setPayPrice(new BigDecimal("199.00"))
                .setFreightPrice(BigDecimal.ZERO)
                .setCreateTime(LocalDateTime.now());
        when(shopOrderMapper.selectByOrderNoAndUid(pendingShipNo, 101L)).thenReturn(pendingShipOrder);
        when(shopOrderStatusService.list(any(ShopOrderStatusDTO.ListQuery.class))).thenReturn(new ArrayList<>());
        when(shopExpressInfoMapper.selectLatestByOid(701L)).thenReturn(null);
        when(shopOrderAfterSaleMapper.selectActiveByOrderId(701L)).thenReturn(null);

        AgentMallVO.OrderDetail pendingShipDetail = agentMallService.getOrderDetail(101L, pendingShipNo);
        assertEquals("待发货", pendingShipDetail.getStatusDesc());

        String refundedNo = "ORDER_REFUNDED";
        ShopOrder refundedOrder = new ShopOrder()
                .setId(702L)
                .setOrderId(refundedNo)
                .setUid(101L)
                .setPaid(1)
                .setStatus(2)
                .setRefundStatus(2)
                .setCartIds("")
                .setPayPrice(new BigDecimal("199.00"))
                .setFreightPrice(BigDecimal.ZERO)
                .setCreateTime(LocalDateTime.now());
        ShopOrderAfterSale afterSale = new ShopOrderAfterSale()
                .setId(30L)
                .setServiceNo("AS001")
                .setOrderId(702L)
                .setOrderNo(refundedNo)
                .setStatus(3);
        when(shopOrderMapper.selectByOrderNoAndUid(refundedNo, 101L)).thenReturn(refundedOrder);
        when(shopExpressInfoMapper.selectLatestByOid(702L)).thenReturn(null);
        when(shopOrderAfterSaleMapper.selectActiveByOrderId(702L)).thenReturn(afterSale);

        AgentMallVO.OrderDetail refundedDetail = agentMallService.getOrderDetail(101L, refundedNo);
        assertEquals("已退款", refundedDetail.getStatusDesc());

        String evaluatedNo = "ORDER_EVALUATED";
        ShopOrder evaluatedOrder = new ShopOrder()
                .setId(703L)
                .setOrderId(evaluatedNo)
                .setUid(101L)
                .setPaid(1)
                .setStatus(2)
                .setCartIds("")
                .setPayPrice(new BigDecimal("199.00"))
                .setFreightPrice(BigDecimal.ZERO)
                .setCreateTime(LocalDateTime.now());
        when(shopOrderMapper.selectByOrderNoAndUid(evaluatedNo, 101L)).thenReturn(evaluatedOrder);
        when(shopExpressInfoMapper.selectLatestByOid(703L)).thenReturn(null);
        when(shopOrderAfterSaleMapper.selectActiveByOrderId(703L)).thenReturn(null);
        assertEquals("待评价", agentMallService.getOrderDetail(101L, evaluatedNo).getStatusDesc());

        String completedNo = "ORDER_COMPLETED";
        ShopOrder completedOrder = new ShopOrder()
                .setId(704L)
                .setOrderId(completedNo)
                .setUid(101L)
                .setPaid(1)
                .setStatus(3)
                .setCartIds("")
                .setPayPrice(new BigDecimal("199.00"))
                .setFreightPrice(BigDecimal.ZERO)
                .setCreateTime(LocalDateTime.now());
        when(shopOrderMapper.selectByOrderNoAndUid(completedNo, 101L)).thenReturn(completedOrder);
        when(shopExpressInfoMapper.selectLatestByOid(704L)).thenReturn(null);
        when(shopOrderAfterSaleMapper.selectActiveByOrderId(704L)).thenReturn(null);
        assertEquals("已完成", agentMallService.getOrderDetail(101L, completedNo).getStatusDesc());
    }

    @Test
    void listUserOrders_refundTabIncludesOrdersBeyond200WithoutTruncation() {
        List<ShopOrder> orders = new ArrayList<>();
        for (int i = 1; i <= 205; i++) {
            orders.add(new ShopOrder()
                    .setId((long) i)
                    .setOrderId("ORDER_" + i)
                    .setUid(101L)
                    .setPaid(1)
                    .setStatus(2)
                    .setPayPrice(new BigDecimal("99.00"))
                    .setCreateTime(LocalDateTime.now().minusMinutes(i)));
        }
        when(shopOrderMapper.selectActiveListByUid(101L)).thenReturn(orders);
        ShopOrderAfterSale afterSale205 = new ShopOrderAfterSale()
                .setId(99L)
                .setServiceNo("AS_205")
                .setOrderId(205L)
                .setOrderNo("ORDER_205")
                .setStatus(0);
        when(shopOrderAfterSaleMapper.selectActiveByOrderId(anyLong())).thenAnswer(inv -> {
            Long orderId = inv.getArgument(0);
            return Long.valueOf(205L).equals(orderId) ? afterSale205 : null;
        });

        PageResult<ShopFrontOrderVO> page = agentMallService.listUserOrders(101L, "refund", null, 1, 5);
        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getRows().size());
        assertEquals("ORDER_205", page.getRows().get(0).getOrderNo());
    }

    @Test
    void getProductDetail_nullsSkuFlashPriceWhenNoActiveFlashSale() {
        ShopProduct product = buildActiveProduct(10L, 1, BigDecimal.ZERO);
        when(shopProductService.getById(10L)).thenReturn(product);

        ShopProductAttrValue sku = buildActiveSku(100L, 10L, new BigDecimal("399.00"), 10);
        sku.setFlashPrice(new BigDecimal("199.00"));
        when(shopProductAttrValueService.getListByProductId(10L)).thenReturn(List.of(sku));

        when(shopFlashSaleService.getAvailableByProductId(eq(10L), any(LocalDateTime.class))).thenReturn(null);
        AgentMallVO.ProductDetail normalDetail = agentMallService.getProductDetail(10L);
        assertFalse(Boolean.TRUE.equals(normalDetail.getActiveFlashSale()));
        assertNull(normalDetail.getSkus().get(0).getFlashPrice());

        ShopFlashSale activeFlashSale = new ShopFlashSale();
        activeFlashSale.setId(88L);
        activeFlashSale.setProductId(10L);
        when(shopFlashSaleService.getAvailableByProductId(eq(10L), any(LocalDateTime.class))).thenReturn(activeFlashSale);
        AgentMallVO.ProductDetail flashDetail = agentMallService.getProductDetail(10L);
        assertTrue(Boolean.TRUE.equals(flashDetail.getActiveFlashSale()));
        assertEquals(0, new BigDecimal("199.00").compareTo(flashDetail.getSkus().get(0).getFlashPrice()));
    }

    private static ShopProduct buildActiveProduct(Long id, int isPostage, BigDecimal postage) {
        ShopProduct product = new ShopProduct();
        product.setId(id);
        product.setProductName("咔嗒机械键盘");
        product.setIsShow(1);
        product.setIsDel(0);
        product.setStock(20);
        product.setPrice(new BigDecimal("399.00"));
        product.setIsPostage(isPostage);
        product.setPostage(postage);
        return product;
    }

    private static ShopProductAttrValue buildActiveSku(Long skuId, Long productId, BigDecimal price, int stock) {
        ShopProductAttrValue sku = new ShopProductAttrValue();
        sku.setId(skuId);
        sku.setProductId(productId);
        sku.setSku("红轴/白");
        sku.setPrice(price);
        sku.setStock(stock);
        sku.setIsDel(0);
        return sku;
    }

    private static AgentMallDTO.DirectCreateOrder buildDirectOrderRequest(Long productId,
                                                                           Long skuId,
                                                                           Integer quantity,
                                                                           Long addressId,
                                                                           String idempotencyKey) {
        AgentMallDTO.DirectCreateOrder dto = new AgentMallDTO.DirectCreateOrder();
        dto.setProductId(productId);
        dto.setSkuId(skuId);
        dto.setQuantity(quantity);
        dto.setAddressId(addressId);
        dto.setIdempotencyKey(idempotencyKey);
        return dto;
    }
}
