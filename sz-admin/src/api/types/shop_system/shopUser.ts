import type { IPageQuery } from '@/api/types';

// 查询条件
export type ShopUserQuery = IPageQuery & {
    account?: string;
    realName?: string;
    sex?: number;
    cardId?: string;
    nickname?: string;
    phone?: string;
    status?: number;
};

// 编辑form表单
export type ShopUserForm = {
    uid?: number;
    account?: string;
    password?: string;
    realName?: string;
    sex?: number;
    birthday?: string;
    cardId?: string;
    nickname?: string;
    avatar?: string;
    phone?: string;
    addIp?: string;
    lastIp?: string;
    nowMoney?: number;
    integral?: number;
    experience?: number;
    signNum?: number;
    level?: number;
    userType?: string;
    status?: number;
};

// list或detail返回结构
export type ShopUserRow = {
    uid?: number;
    account?: string;
    password?: string;
    realName?: string;
    sex?: number;
    birthday?: string;
    cardId?: string;
    nickname?: string;
    avatar?: string;
    phone?: string;
    addIp?: string;
    lastIp?: string;
    nowMoney?: number;
    integral?: number;
    experience?: number;
    signNum?: number;
    level?: number;
    userType?: string;
    createTime?: string;
    updateTime?: string;
    status?: number;
};

