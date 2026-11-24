import type { IPageQuery } from '@/api/types';

// 查询条件
export type ShopUserBillQuery = IPageQuery & {
    uid?: number;
    account?: string;
    realName?: string;
    linkId?: string;
    pm?: number | string;
    title?: string;
    category?: string;
    type?: string;
    number?: number;
    balance?: number;
    mark?: string;
    status?: number;
};

// 编辑form表单
export type ShopUserBillForm = {
};

// list或detail返回结构
export type ShopUserBillRow = {
    id?: number;
    uid?: number;
    account?: string;
    realName?: string;
    pm?: number | string;
    title?: string;
    category?: string;
    type?: string;
    number?: number;
    balance?: number;
    mark?: string;
    createTime?: string;
    updateTime?: string;
    status?: number;
};
