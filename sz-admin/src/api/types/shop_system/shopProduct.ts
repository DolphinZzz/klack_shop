import type { IPageQuery } from '@/api/types';

// 查询条件
export type ShopProductQuery = IPageQuery & {
    productName?: string;
    cateId?: number;
    isShow?: number;
};

// 编辑form表单
export type ShopProductForm = {
    id?: number;
    image?: string;
    productName?: string;
    productInfo?: string;
    keyword?: string;
    cateId?: number;
    price?: number;
    vipPrice?: number;
    otPrice?: number;
    isPostage?: number;
    postage?: number;
    unitName?: string;
    sales?: number;
    stock?: number;
    isShow?: number;
    isHot?: number;
    isBest?: number;
    isNew?: number;
    isSeckill?: number;
    isBargain?: number;
    giveIntegral?: number;
    desc?: string;
    activity?: string;
    isDel?: number;
};

// list或detail返回结构
export type ShopProductRow = {
    id?: number;
    image?: string;
    productName?: string;
    price?: number;
    sales?: number;
    stock?: number;
    isShow?: number;
    isSeckill?: number;
    createTime?: string;
    updateTime?: string;
};

