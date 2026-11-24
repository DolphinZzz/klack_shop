package com.sz.admin.shop_system.constants;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author car
 * @create 2026/2/25
 */
public final class ShopPaymentConstant {
    private ShopPaymentConstant() {
    }

    public static final Integer PM_PAY = 0;
    public static final Integer PM_REFUND = 1;

    public static final String CATEGORY_NOW_MONEY = "now_money";
    public static final String CATEGORY_INTEGRAL = "integral";

    public static final String TYPE_PAY_ORDER = "pay_order";
    public static final String TYPE_PAY_PRODUCT_REFUND = "pay_product_refund";
    public static final String TYPE_SYSTEM_SUB = "system_sub";
    public static final String TYPE_RECHARGE = "recharge";
    public static final String TYPE_USE_INTEGRAL_ORDER = "use_integral_order";
    public static final String TYPE_GAIN_INTEGRAL_ORDER = "gain_integral_order";
}
