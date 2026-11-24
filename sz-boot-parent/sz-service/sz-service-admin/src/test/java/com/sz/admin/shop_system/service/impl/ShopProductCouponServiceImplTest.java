package com.sz.admin.shop_system.service.impl;

import com.sz.admin.shop_system.mapper.ShopProductCouponMapper;
import com.sz.admin.shop_system.mapper.ShopProductCouponUserMapper;
import com.sz.admin.shop_system.pojo.dto.ShopProductCouponDTO;
import com.sz.admin.shop_system.pojo.po.ShopProductCoupon;
import com.sz.admin.shop_system.pojo.po.ShopProductCouponUser;
import com.sz.admin.shop_system.service.ShopProductCouponUserService;
import com.sz.core.common.exception.common.BusinessException;
import com.sz.security.util.StpUserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopProductCouponServiceImplTest {

    @Mock
    private ShopProductCouponMapper couponMapper;
    @Mock
    private ShopProductCouponUserMapper couponUserMapper;
    @Mock
    private ShopProductCouponUserService couponUserService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private TransactionTemplate transactionTemplate;

    private ShopProductCouponServiceImpl service;
    private MockedStatic<StpUserUtil> stpUserUtilMock;

    @BeforeEach
    void setUp() {
        service = new ShopProductCouponServiceImpl(
                couponUserMapper,
                couponUserService,
                stringRedisTemplate,
                transactionTemplate
        );
        ReflectionTestUtils.setField(service, "mapper", couponMapper);
        ReflectionTestUtils.invokeMethod(service, "startOrderWorker");

        stpUserUtilMock = mockStatic(StpUserUtil.class);
        stpUserUtilMock.when(StpUserUtil::getLoginIdAsLong).thenReturn(1001L);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.invokeMethod(service, "stopOrderWorker");
        if (stpUserUtilMock != null) {
            stpUserUtilMock.close();
        }
    }

    @Test
    void createShouldSaveCouponStockToRedis() {
        ShopProductCouponDTO.Create dto = new ShopProductCouponDTO.Create();
        dto.setName("秒杀满减券");
        dto.setMoney(new BigDecimal("20"));
        dto.setMinMoney(new BigDecimal("10"));
        dto.setTotal(50);
        dto.setType(1);
        dto.setCateKey("0");
        dto.setCouponStartTime(LocalDateTime.now().plusMinutes(1));
        dto.setCouponEndTime(LocalDateTime.now().plusDays(1));
        dto.setUseStartTime(LocalDateTime.now().plusMinutes(1));
        dto.setUseEndTime(LocalDateTime.now().plusDays(2));

        when(couponMapper.insert(any(ShopProductCoupon.class), eq(true))).thenAnswer(invocation -> {
            ShopProductCoupon entity = invocation.getArgument(0);
            entity.setId(88L);
            return 1;
        });
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        service.create(dto);

        verify(valueOperations).set("coupon:vid:88", "50");
    }

    @Test
    void grabCouponShouldReturnOrderIdAndPersistAsynchronouslyWhenLuaReturnsZero() throws Exception {
        long couponId = 88L;
        CountDownLatch persistedLatch = new CountDownLatch(1);

        when(stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(Arrays.asList("coupon:vid:88", "order:vid:88")),
                eq("1001")
        )).thenReturn(0L);

        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        when(couponUserMapper.countByCidAndUid(couponId, 1001L)).thenReturn(0L);
        ShopProductCoupon coupon = new ShopProductCoupon();
        coupon.setId(couponId);
        coupon.setType(1);
        coupon.setStatus(0);
        coupon.setCateKey("0");
        when(couponMapper.selectOneById(couponId)).thenReturn(coupon);
        when(couponMapper.deductLastTotalAtomic(couponId)).thenReturn(1);
        when(couponUserService.save(any(ShopProductCouponUser.class))).thenAnswer(invocation -> {
            persistedLatch.countDown();
            return true;
        });

        String orderId = service.grabCoupon(couponId);

        assertNotNull(orderId);
        assertTrue(orderId.startsWith("ORDCP"));
        assertTrue(persistedLatch.await(3, TimeUnit.SECONDS), "异步线程应在超时前完成落库");

        verify(couponMapper).deductLastTotalAtomic(couponId);
        ArgumentCaptor<ShopProductCouponUser> captor = ArgumentCaptor.forClass(ShopProductCouponUser.class);
        verify(couponUserService).save(captor.capture());
        assertEquals(couponId, captor.getValue().getCid());
        assertEquals(1001L, captor.getValue().getUid());
        assertEquals("get", captor.getValue().getType());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getUseType());
    }

    @Test
    void grabCouponShouldRejectWithoutTouchingDbWhenStockInsufficient() {
        when(stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(Arrays.asList("coupon:vid:88", "order:vid:88")),
                eq("1001")
        )).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.grabCoupon(88L));
        verifyNoInteractions(couponMapper, couponUserMapper, couponUserService, transactionTemplate);
    }

    @Test
    void grabCouponShouldRejectWithoutTouchingDbWhenDuplicateUser() {
        when(stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(Arrays.asList("coupon:vid:88", "order:vid:88")),
                eq("1001")
        )).thenReturn(2L);

        assertThrows(BusinessException.class, () -> service.grabCoupon(88L));
        verifyNoInteractions(couponMapper, couponUserMapper, couponUserService, transactionTemplate);
    }

    @Test
    void asyncWorkerShouldRollbackRedisWhenDbPersistenceFails() throws Exception {
        long couponId = 99L;
        CountDownLatch rollbackLatch = new CountDownLatch(1);

        when(stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(Arrays.asList("coupon:vid:99", "order:vid:99")),
                eq("1001")
        )).thenReturn(0L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);

        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        when(couponUserMapper.countByCidAndUid(couponId, 1001L)).thenReturn(0L);
        ShopProductCoupon coupon = new ShopProductCoupon();
        coupon.setId(couponId);
        coupon.setType(1);
        coupon.setStatus(0);
        when(couponMapper.selectOneById(couponId)).thenReturn(coupon);
        when(couponMapper.deductLastTotalAtomic(couponId)).thenReturn(0);
        when(setOperations.remove("order:vid:99", "1001")).thenAnswer(invocation -> {
            rollbackLatch.countDown();
            return 1L;
        });

        String orderId = service.grabCoupon(couponId);

        assertNotNull(orderId);
        assertTrue(rollbackLatch.await(3, TimeUnit.SECONDS), "落库失败后应回滚 Redis 预扣库存与 Set");
        verify(valueOperations).increment("coupon:vid:99", 1L);
        verify(setOperations).remove("order:vid:99", "1001");
    }
}
