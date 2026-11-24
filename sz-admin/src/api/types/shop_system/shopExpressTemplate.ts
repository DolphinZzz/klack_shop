import type { IPageQuery } from '@/api/types';

// 查询条件
export type ShopExpressTemplateQuery = IPageQuery & {
    name?: string;
    expressPrice?: number;
};

// 编辑form表单
export type ShopExpressTemplateForm = {
    id?: number;
    name?: string;
    expressId?: number;
    expressPrice?: number;
    isDel?: number;
};

// list或detail返回结构
export interface ShopExpressTemplateRow {
  id: number;
  name: string;
  expressId: number;
  code: string;
  expressName: string;
  sort: number;
  createTime: string;
  updateTime: string;
  expressPrice: number;
  isDel: number | string; // 后端可能是 ""，先兼容
}

