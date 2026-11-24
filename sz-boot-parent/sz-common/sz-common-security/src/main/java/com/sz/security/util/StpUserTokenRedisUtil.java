package com.sz.security.util;

import com.sz.core.common.enums.CommonResponseEnum;
import com.sz.redis.CommonKeyConstants;
import com.sz.redis.RedisUtils;

import java.util.concurrent.TimeUnit;

/**
 * shop_user token redis persistence and validation helper.
 */
public class StpUserTokenRedisUtil {

    private StpUserTokenRedisUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static void saveToken(Long userId, String tokenValue, long timeoutSeconds) {
        if (userId == null || isBlank(tokenValue)) {
            return;
        }
        long ttl = timeoutSeconds > 0 ? timeoutSeconds : 604800L;
        String key = RedisUtils.getKey(CommonKeyConstants.SHOP_USER_TOKEN, tokenValue);
        RedisUtils.getRestTemplate().opsForValue().set(key, userId, ttl, TimeUnit.SECONDS);
    }

    public static void removeToken(String tokenValue) {
        if (isBlank(tokenValue)) {
            return;
        }
        RedisUtils.removeKey(CommonKeyConstants.SHOP_USER_TOKEN, tokenValue);
    }

    public static void checkToken() {
        String tokenValue = StpUserUtil.getTokenValue();
        CommonResponseEnum.INVALID_TOKEN.assertTrue(isBlank(tokenValue));

        String key = RedisUtils.getKey(CommonKeyConstants.SHOP_USER_TOKEN, tokenValue);
        CommonResponseEnum.INVALID_TOKEN.assertTrue(!RedisUtils.hasKey(key));

        Long ttl = RedisUtils.getRestTemplate().getExpire(key, TimeUnit.SECONDS);
        CommonResponseEnum.INVALID_TOKEN.assertTrue(ttl == null || ttl <= 0);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
