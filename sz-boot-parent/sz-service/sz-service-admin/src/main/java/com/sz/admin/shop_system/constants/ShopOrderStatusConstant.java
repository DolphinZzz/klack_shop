package com.sz.admin.shop_system.constants;

/**
 * 订单状态变更常量
 */
public final class ShopOrderStatusConstant {

    private ShopOrderStatusConstant() {
    }

    public static final String CHANGE_TYPE_CREATE_ORDER = "create_order";
    public static final String CHANGE_TYPE_PAY_SUCCESS = "pay_success";
    public static final String CHANGE_TYPE_DELIVERY = "delivery";
    public static final String CHANGE_TYPE_USER_TAKE_DELIVERY = "user_take_delivery";
    public static final String CHANGE_TYPE_ORDER_EVAL = "order_eval";
    public static final String CHANGE_TYPE_CANCEL_ORDER = "cancel_order";
    public static final String CHANGE_TYPE_REFUNDING = "refunding";
    public static final String CHANGE_TYPE_REFUNDED = "refunded";
    public static final String CHANGE_TYPE_CLOSED = "closed";
    public static final String CHANGE_TYPE_FLASH_RESERVE = "flash_reserve";
    public static final String CHANGE_TYPE_FLASH_SUBMIT = "flash_submit";
    public static final String CHANGE_TYPE_FLASH_RELEASE = "flash_release";
    public static final String CHANGE_TYPE_AFTER_SALE_APPLY = "after_sale_apply";
    public static final String CHANGE_TYPE_AFTER_SALE_APPROVE = "after_sale_approve";
    public static final String CHANGE_TYPE_AFTER_SALE_REJECT = "after_sale_reject";
    public static final String CHANGE_TYPE_AFTER_SALE_RETURN = "after_sale_return";
    public static final String CHANGE_TYPE_AFTER_SALE_REFUND = "after_sale_refund";

    public static final String CHANGE_MESSAGE_CREATE_ORDER = "订单生成";
    public static final String CHANGE_MESSAGE_PAY_SUCCESS = "用户付款成功";
    public static final String CHANGE_MESSAGE_DELIVERY_TEMPLATE = "已发货：快递公司为：%s";
    public static final String CHANGE_MESSAGE_USER_TAKE_DELIVERY = "用户已收货";
    public static final String CHANGE_MESSAGE_ORDER_EVAL = "用户已评价";
    public static final String CHANGE_MESSAGE_CANCEL_ORDER = "订单已取消";
    public static final String CHANGE_MESSAGE_REFUNDING = "申请退款";
    public static final String CHANGE_MESSAGE_REFUNDED = "退款成功";
    public static final String CHANGE_MESSAGE_CLOSED = "订单已关闭";
    public static final String CHANGE_MESSAGE_FLASH_RESERVE = "秒杀资格已锁定，请尽快提交订单";
    public static final String CHANGE_MESSAGE_FLASH_SUBMIT = "秒杀订单已提交，等待支付";
    public static final String CHANGE_MESSAGE_FLASH_RELEASE = "秒杀超时未完成，库存已释放";
    public static final String CHANGE_MESSAGE_AFTER_SALE_APPLY = "用户发起售后申请";
    public static final String CHANGE_MESSAGE_AFTER_SALE_APPROVE = "商家已同意售后申请";
    public static final String CHANGE_MESSAGE_AFTER_SALE_REJECT = "商家已拒绝售后申请";
    public static final String CHANGE_MESSAGE_AFTER_SALE_RETURN = "用户已寄回退货商品";
    public static final String CHANGE_MESSAGE_AFTER_SALE_REFUND = "售后退款完成";
}
