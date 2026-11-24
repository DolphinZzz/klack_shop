import http from '@/api'; // 按你项目封装
import type { PageResult } from '@/api/interface'; // 按你项目的分页类型

export interface ShopExpressRow {
  id: number;
  code: string;
  expressName: string;
  sort: number;
  createTime: string;
  updateTime: string;
  isDel: number;
}

export interface ShopExpressQuery {
  current?: number;
  limit?: number;
  code?: string;
  expressName?: string;
}

export const getShopExpressListApi = (params: ShopExpressQuery) => {
  return http.get<PageResult<ShopExpressRow>>('/server/shopExpress', params);
};

export const createShopExpressApi = (data: any) => {
  return http.post('/server/shopExpress', data);
};

export const removeShopExpressApi = (data: { ids: (number | string)[] }) => {
  return http.delete('/server/shopExpress', data);
};
