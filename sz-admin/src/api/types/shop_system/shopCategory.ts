import type { IPageQuery } from '@/api/types';

// 查询条件
export type ShopCategoryQuery = IPageQuery & {
    cateName?: string;
};

// 编辑form表单
export type ShopCategoryForm = {
    cateName?: string;
    pid?: number | string;
};

// list或detail返回结构
export type ShopCategoryRow = {
    id?: number | string;
    cateName?: string;
    pid?: number | string;
    children?: ShopCategoryRow[];
    [key: string]: any;
};

