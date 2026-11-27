package com.sz.admin.shop_system.mq;

import com.sz.admin.shop_system.mapper.ShopProductCouponMapper;
import com.sz.admin.shop_system.mapper.ShopProductCouponUserMapper;
import com.sz.admin.shop_system.pojo.po.ShopProductCoupon;
import com.sz.admin.shop_system.pojo.po.ShopProductCouponUser;
import com.sz.admin.shop_system.service.ShopProductCouponUserService;
import com.sz.mq.core.MqMessage;
import com.sz.mq.core.MqNonRetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponSeckillOrderConsumerTest {

    @Mock
    private ShopProductCouponMapper couponMapper;
    @Mock
    private ShopProductCouponUserMapper couponUserMapper;
    @Mock
    private ShopProductCouponUserService couponUserService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private CouponSeckillRedisHelper redisHelper;

    private CouponSeckillOrderConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CouponSeckillOrderConsumer(
                couponMapper,
                couponUserMapper,
                couponUserService,
                transactionTemplate,
                redisHelper
        );
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void shouldPersistOrderInTransactionWhenValid() {
        long couponId = 88L;
        long userId = 1001L;
        when(couponUserMapper.countByCidAndUid(couponId, userId)).thenReturn(0L);
        ShopProductCoupon coupon = activeCoupon(couponId);
        when(couponMapper.selectOneById(couponId)).thenReturn(coupon);
        when(couponMapper.deductLastTotalAtomic(couponId)).thenReturn(1);
        when(couponUserService.save(any(ShopProductCouponUser.class))).thenReturn(true);

        consumer.onMessage(seckillMessage("ORDCP1", couponId, userId));

        verify(couponMapper).deductLastTotalAtomic(couponId);
        ArgumentCaptor<ShopProductCouponUser> captor = ArgumentCaptor.forClass(ShopProductCouponUser.class);
        verify(couponUserService).save(captor.capture());
        assertEquals(couponId, captor.getValue().getCid());
        assertEquals(userId, captor.getValue().getUid());
        assertEquals("get", captor.getValue().getType());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getUseType());
        verifyNoInteractions(redisHelper);
    }

    @Test
    void shouldSkipIdempotentlyWhenUserRecordAlreadyExists() {
        long couponId = 88L;
        long userId = 1001L;
        when(couponUserMapper.countByCidAndUid(couponId, userId)).thenReturn(1L);

        consumer.onMessage(seckillMessage("ORDCP2", couponId, userId));

        verify(couponMapper, never()).deductLastTotalAtomic(anyLong());
        verify(couponUserService, never()).save(any());
        verifyNoInteractions(redisHelper);
    }

    @Test
    void shouldThrowNonRetryableExceptionWhenCouponMissingOrDisabledOrNoDbStock() {
        long couponId = 99L;
        long userId = 1001L;
        when(couponUserMapper.countByCidAndUid(couponId, userId)).thenReturn(0L);

        // 1. 优惠券不存在
        when(couponMapper.selectOneById(couponId)).thenReturn(null);
        assertThrows(MqNonRetryableException.class,
                () -> consumer.onMessage(seckillMessage("ORDCP3", couponId, userId)));

        // 2. 优惠券被禁用
        ShopProductCoupon disabled = activeCoupon(couponId);
        disabled.setStatus(1);
        when(couponMapper.selectOneById(couponId)).thenReturn(disabled);
        assertThrows(MqNonRetryableException.class,
                () -> consumer.onMessage(seckillMessage("ORDCP4", couponId, userId)));

        // 3. 数据库库存原子扣减返回 0
        ShopProductCoupon valid = activeCoupon(couponId);
        when(couponMapper.selectOneById(couponId)).thenReturn(valid);
        when(couponMapper.deductLastTotalAtomic(couponId)).thenReturn(0);
        assertThrows(MqNonRetryableException.class,
                () -> consumer.onMessage(seckillMessage("ORDCP5", couponId, userId)));
    }

    @Test
    void shouldPropagateTransientDbExceptionForRetry() {
        long couponId = 88L;
        long userId = 1001L;
        when(couponUserMapper.countByCidAndUid(couponId, userId)).thenThrow(new RuntimeException("MySQL connection timeout"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> consumer.onMessage(seckillMessage("ORDCP6", couponId, userId)));
        assertFalse(ex instanceof MqNonRetryableException);
    }

    @Test
    void shouldRollbackRedisOnlyWhenNonRetryableFailure() {
        MqMessage<CouponSeckillMessage> msg = seckillMessage("ORDCP7", 88L, 1001L);

        consumer.onConsumeFailed(msg, new MqNonRetryableException("coupon disabled"), false);
        verify(redisHelper, times(1)).rollbackSeckill(88L, 1001L);
    }

    @Test
    void shouldNotRollbackRedisWhenTransientFailureExhaustedForManualDlqReplay() {
        MqMessage<CouponSeckillMessage> msg = seckillMessage("ORDCP8", 88L, 1001L);

        consumer.onConsumeFailed(msg, new RuntimeException("DB lock wait timeout"), true);
        verify(redisHelper, never()).rollbackSeckill(anyLong(), anyLong());
    }

    private ShopProductCoupon activeCoupon(long id) {
        ShopProductCoupon coupon = new ShopProductCoupon();
        coupon.setId(id);
        coupon.setType(1);
        coupon.setStatus(0);
        coupon.setCateKey("0");
        coupon.setCouponStartTime(LocalDateTime.now().minusMinutes(5));
        coupon.setCouponEndTime(LocalDateTime.now().plusHours(1));
        coupon.setUseEndTime(LocalDateTime.now().plusDays(1));
        return coupon;
    }

    private MqMessage<CouponSeckillMessage> seckillMessage(String orderId, Long couponId, Long userId) {
        CouponSeckillMessage payload = CouponSeckillMessage.builder()
                .orderId(orderId)
                .couponId(couponId)
                .userId(userId)
                .requestTimeMillis(System.currentTimeMillis())
                .build();
        return MqMessage.of(CouponSeckillMqConstants.TOPIC, CouponSeckillMqConstants.TAG_GRAB, orderId, payload);
    }
}
