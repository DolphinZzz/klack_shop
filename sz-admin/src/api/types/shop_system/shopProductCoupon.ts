import type { IPageQuery } from '@/api/types';

// 查询条件
export type ShopProductCouponQuery = IPageQuery & {
    name?: string;
    type?: number;
    status?: number;
};

// 编辑form表单
export type ShopProductCouponForm = {
    name?: string;
    money?: number;
    total?: number;
    couponStartTime?: string;
    couponEndTime?: string;
    useStartTime?: string;
    useEndTime?: string;
    type?: number;
    cateKey?: string;
    minMoney?: number;
};

// list或detail返回结构
export type ShopProductCouponRow = {
    id?: number;
    name?: string;
    money?: number;
    total?: number;
    lastTotal?: number;
    type?: number;
    cateKey?: string;
    minMoney?: number;
};

