import type { IPageQuery } from '@/api/types';

// 查询条件
export type ShopUserRechargeActivityQuery = IPageQuery & {
    activityName?: string;
};

// 编辑form表单
export type ShopUserRechargeActivityForm = {
    id?: number;
    activityName?: string;
    price?: number;
    givePrice?: number;
};

// list或detail返回结构
export type ShopUserRechargeActivityRow = {
    id?: number;
    activityName?: string;
    price?: number;
    givePrice?: number;
};

