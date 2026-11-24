package com.sz.admin.shop_system.constants;

public final class ShopOrderAfterSaleConstant {

    private ShopOrderAfterSaleConstant() {
    }

    public static final Integer TYPE_REFUND_ONLY = 1;
    public static final Integer TYPE_RETURN_REFUND = 2;

    public static final Integer STATUS_WAIT_AUDIT = 0;
    public static final Integer STATUS_WAIT_BUYER_RETURN = 1;
    public static final Integer STATUS_WAIT_MERCHANT_RECEIVE = 2;
    public static final Integer STATUS_REFUND_SUCCESS = 3;
    public static final Integer STATUS_REJECTED = 4;
}