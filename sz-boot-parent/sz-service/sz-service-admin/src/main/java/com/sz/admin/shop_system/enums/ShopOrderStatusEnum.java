package com.sz.admin.shop_system.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum ShopOrderStatusEnum {
    CREATED(0, "订单生成"),
    PAID(1, "付款成功"),
    DELIVERED(2, "已发货"),
    RECEIVED(3, "已收货"),
    EVALUATED(4, "已评价"),
    CANCELLED(5, "已取消"),
    REFUNDING(6, "退款中"),
    REFUNDED(7, "已退款"),
    CLOSED(8, "已关闭");

    private static final Map<Integer, Set<Integer>> ALLOWED_TRANSITIONS;

    static {
        Map<Integer, Set<Integer>> map = new HashMap<>();
        map.put(CREATED.code, setOf(PAID.code, CANCELLED.code));
        map.put(PAID.code, setOf(DELIVERED.code, REFUNDING.code));
        map.put(DELIVERED.code, setOf(RECEIVED.code, REFUNDING.code));
        map.put(RECEIVED.code, setOf(EVALUATED.code, REFUNDING.code));
        map.put(REFUNDING.code, setOf(REFUNDED.code));
        map.put(REFUNDED.code, setOf(CLOSED.code));
        map.put(CANCELLED.code, setOf(CLOSED.code));
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private final int code;
    private final String desc;

    ShopOrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ShopOrderStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ShopOrderStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public static boolean canTransfer(int fromCode, int toCode) {
        Set<Integer> targets = ALLOWED_TRANSITIONS.get(fromCode);
        return targets != null && targets.contains(toCode);
    }

    private static Set<Integer> setOf(Integer... values) {
        Set<Integer> set = new HashSet<>();
        Collections.addAll(set, values);
        return set;
    }
}
