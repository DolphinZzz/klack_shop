package com.sz.admin.shop_system.enums;

import com.sz.admin.shop_system.constants.ShopOrderAfterSaleConstant;

public enum ShopOrderAfterSaleTypeEnum {
    REFUND_ONLY(ShopOrderAfterSaleConstant.TYPE_REFUND_ONLY, "仅退款"),
    RETURN_REFUND(ShopOrderAfterSaleConstant.TYPE_RETURN_REFUND, "退货退款");

    private final int code;
    private final String desc;

    ShopOrderAfterSaleTypeEnum(int code, String desc) {
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
        for (ShopOrderAfterSaleTypeEnum value : values()) {
            if (value.code == code) {
                return value.desc;
            }
        }
        return "-";
    }
}