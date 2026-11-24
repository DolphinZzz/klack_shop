package com.sz.admin.shop_system.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sz.payment")
public class ShopPaymentProperties {

    /**
     * 支付超时时长，单位：分钟
     */
    private long timeoutMinutes = 30L;

    /**
     * 是否显示微信支付
     */
    private boolean enableWxpay = false;

    /**
     * 是否显示支付宝支付
     */
    private boolean enableAlipay = false;

    /**
     * 是否显示余额支付
     */
    private boolean enableYue = true;
}
