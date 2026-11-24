import type { IPageQuery } from '@/api/types';

// 查询条件
export type ShopHomeBannerQuery = IPageQuery & {
    title?: string;
};

// 编辑form表单
export type ShopHomeBannerForm = {
    id?: number;
    title?: string;
    image?: string;
    link?: string;
    sort?: number;
    status?: number;
    startTime?: string;
    endTime?: string;
    remark?: string;
};

// list或detail返回结构
export type ShopHomeBannerRow = {
    id?: number;
    title?: string;
    image?: string;
    link?: string;
    sort?: number;
    status?: number;
    startTime?: string;
    endTime?: string;
    remark?: string;
};

