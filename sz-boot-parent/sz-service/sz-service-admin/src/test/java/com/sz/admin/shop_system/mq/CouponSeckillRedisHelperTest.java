package com.sz.admin.shop_system.mq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponSeckillRedisHelperTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private CouponSeckillRedisHelper helper;

    @BeforeEach
    void setUp() {
        helper = new CouponSeckillRedisHelper(stringRedisTemplate);
    }

    @Test
    void shouldExecutePreDeductScriptWithExpectedKeys() {
        when(stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(Arrays.asList("coupon:vid:88", "order:vid:88")),
                eq("1001")
        )).thenReturn(0L);

        Long result = helper.tryPreDeduct(88L, 1001L);
        assertEquals(0L, result);
    }

    @Test
    void shouldExecuteIdempotentSafeRollbackLuaAndHandleAllReturnCodes() {
        when(stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(Arrays.asList("coupon:vid:88", "order:vid:88")),
                eq("1001")
        )).thenReturn(1L, 2L, 0L);

        assertDoesNotThrow(() -> helper.rollbackSeckill(88L, 1001L));
        assertDoesNotThrow(() -> helper.rollbackSeckill(88L, 1001L));
        assertDoesNotThrow(() -> helper.rollbackSeckill(88L, 1001L));
        verify(stringRedisTemplate, org.mockito.Mockito.times(3))
                .execute(any(RedisScript.class), eq(Arrays.asList("coupon:vid:88", "order:vid:88")), eq("1001"));
    }

    @Test
    void shouldSwallowRedisExceptionDuringRollback() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> helper.rollbackSeckill(88L, 1001L));
    }
}
