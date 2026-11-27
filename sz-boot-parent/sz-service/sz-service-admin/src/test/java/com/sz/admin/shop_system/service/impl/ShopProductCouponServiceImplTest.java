package com.sz.admin.shop_system.service.impl;

import com.sz.admin.shop_system.mapper.ShopProductCouponMapper;
import com.sz.admin.shop_system.mapper.ShopProductCouponUserMapper;
import com.sz.admin.shop_system.mq.CouponSeckillMessage;
import com.sz.admin.shop_system.mq.CouponSeckillMqConstants;
import com.sz.admin.shop_system.mq.CouponSeckillRedisHelper;
import com.sz.admin.shop_system.pojo.dto.ShopProductCouponDTO;
import com.sz.admin.shop_system.pojo.po.ShopProductCoupon;
import com.sz.core.common.exception.common.BusinessException;
import com.sz.mq.core.MqSendException;
import com.sz.mq.core.MqSendResult;
import com.sz.mq.core.MqTypeEnum;
import com.sz.mq.strategy.MqTemplate;
import com.sz.security.util.StpUserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private MqTemplate mqTemplate;
    @Mock
    private CouponSeckillRedisHelper couponSeckillRedisHelper;

    private ShopProductCouponServiceImpl service;
    private MockedStatic<StpUserUtil> stpUserUtilMock;

    @BeforeEach
    void setUp() {
        lenient().when(couponSeckillRedisHelper.buildStockKey(anyLong()))
                .thenAnswer(inv -> "coupon:vid:" + inv.getArgument(0));
        lenient().when(couponSeckillRedisHelper.buildOrderKey(anyLong()))
                .thenAnswer(inv -> "order:vid:" + inv.getArgument(0));
        lenient().when(mqTemplate.currentType()).thenReturn(MqTypeEnum.RABBITMQ);

        service = new ShopProductCouponServiceImpl(
                couponUserMapper,
                stringRedisTemplate,
                mqTemplate,
                couponSeckillRedisHelper
        );
        ReflectionTestUtils.setField(service, "mapper", couponMapper);

        stpUserUtilMock = mockStatic(StpUserUtil.class);
        stpUserUtilMock.when(StpUserUtil::getLoginIdAsLong).thenReturn(1001L);
    }

    @AfterEach
    void tearDown() {
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
    void grabCouponShouldSendMqMessageAndReturnOrderIdWhenLuaReturnsZero() {
        long couponId = 88L;
        when(couponSeckillRedisHelper.tryPreDeduct(couponId, 1001L)).thenReturn(0L);
        when(mqTemplate.send(eq(CouponSeckillMqConstants.TOPIC), eq(CouponSeckillMqConstants.TAG_GRAB), anyString(), any()))
                .thenReturn(MqSendResult.builder().success(true).mqType(MqTypeEnum.RABBITMQ).build());

        String orderId = service.grabCoupon(couponId);

        assertNotNull(orderId);
        assertTrue(orderId.startsWith("ORDCP"));

        ArgumentCaptor<CouponSeckillMessage> msgCaptor = ArgumentCaptor.forClass(CouponSeckillMessage.class);
        verify(mqTemplate).send(
                eq(CouponSeckillMqConstants.TOPIC),
                eq(CouponSeckillMqConstants.TAG_GRAB),
                eq(orderId),
                msgCaptor.capture()
        );
        assertEquals(orderId, msgCaptor.getValue().getOrderId());
        assertEquals(couponId, msgCaptor.getValue().getCouponId());
        assertEquals(1001L, msgCaptor.getValue().getUserId());
        verify(couponSeckillRedisHelper, never()).rollbackSeckill(anyLong(), anyLong());
    }

    @Test
    void grabCouponShouldRollbackRedisWhenMqSendFails() {
        long couponId = 99L;
        when(couponSeckillRedisHelper.tryPreDeduct(couponId, 1001L)).thenReturn(0L);
        when(mqTemplate.send(eq(CouponSeckillMqConstants.TOPIC), eq(CouponSeckillMqConstants.TAG_GRAB), anyString(), any()))
                .thenThrow(new MqSendException("RabbitMQ broker down"));

        assertThrows(BusinessException.class, () -> service.grabCoupon(couponId));
        verify(couponSeckillRedisHelper, times(1)).rollbackSeckill(couponId, 1001L);
    }

    @Test
    void grabCouponShouldRejectWithoutSendingMqWhenStockInsufficient() {
        when(couponSeckillRedisHelper.tryPreDeduct(88L, 1001L)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.grabCoupon(88L));
        verifyNoInteractions(mqTemplate, couponMapper, couponUserMapper);
    }

    @Test
    void grabCouponShouldRejectWithoutSendingMqWhenDuplicateUser() {
        when(couponSeckillRedisHelper.tryPreDeduct(88L, 1001L)).thenReturn(2L);

        assertThrows(BusinessException.class, () -> service.grabCoupon(88L));
        verifyNoInteractions(mqTemplate, couponMapper, couponUserMapper);
    }
}
