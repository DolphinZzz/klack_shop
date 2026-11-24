package com.sz.admin.shop_system.constants;

/**
 * Created by IntelliJ IDEA.
 * @Author car
 * @create 2025/12/4
 */public class Constant {
    //默认分页
    public static final int DEFAULT_PAGE = 1;

    //默认分页
    public static final int DEFAULT_LIMIT = 20;

    //升序排序
    public static final String SORT_ASC = "asc";

    //降序排序
    public static final String SORT_DESC = "desc";

    // 商品类型 活动类型 0=商品，1=秒杀，2=砍价，3=拼团 attrResult表用到
    public static final Integer PRODUCT_TYPE_NORMAL = 0;
    public static final String PRODUCT_TYPE_NORMAL_STR = "默认";
    public static final Integer PRODUCT_TYPE_SECKILL = 1;
    public static final String PRODUCT_TYPE_SECKILL_STR = "秒杀";
    public static final Integer PRODUCT_TYPE_BARGAIN = 2;
    public static final String PRODUCT_TYPE_BARGAIN_STR = "砍价";
    public static final Integer PRODUCT_TYPE_PINGTUAN= 3;
    public static final String PRODUCT_TYPE_PINGTUAN_STR= "拼团";
}


