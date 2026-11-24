package com.sz.admin.shop_system.enums;

import com.sz.admin.shop_system.constants.ShopOrderAfterSaleConstant;

public enum ShopOrderAfterSaleStatusEnum {
    WAIT_AUDIT(ShopOrderAfterSaleConstant.STATUS_WAIT_AUDIT, "待审核"),
    WAIT_BUYER_RETURN(ShopOrderAfterSaleConstant.STATUS_WAIT_BUYER_RETURN, "待寄回"),
    WAIT_MERCHANT_RECEIVE(ShopOrderAfterSaleConstant.STATUS_WAIT_MERCHANT_RECEIVE, "待退款"),
    REFUND_SUCCESS(ShopOrderAfterSaleConstant.STATUS_REFUND_SUCCESS, "已退款"),
    REJECTED(ShopOrderAfterSaleConstant.STATUS_REJECTED, "已拒绝");

    private final int code;
    private final String desc;

    ShopOrderAfterSaleStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static String getDesc(Integer code) {
        if (code == null) {
            return "-";
        }
        for (ShopOrderAfterSaleStatusEnum value : values()) {
            if (value.code == code) {
                return value.desc;
            }
        }
        return "-";
    }
}