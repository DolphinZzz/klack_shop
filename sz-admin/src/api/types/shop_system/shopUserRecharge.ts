import type { IPageQuery } from '@/api/types';

// 查询条件
export type ShopUserRechargeQuery = IPageQuery & {
    uid?: number;
    payActivityId?: number;
    orderId?: number;
    price?: number;
    givePrice?: number;
    rechargeType?: string;
    payTimeStart?: string;
    payTimeEnd?: string;
    refundPrice?: number;
    isDel?: number;
};

// 编辑form表单
export type ShopUserRechargeForm = {
};

// list或detail返回结构
export type ShopUserRechargeRow = {
    id?: number;
    uid?: number;
    payActivityId?: number;
    orderId?: number;
    price?: number;
    givePrice?: number;
    rechargeType?: string;
    payTime?: string;
    refundPrice?: number;
};

