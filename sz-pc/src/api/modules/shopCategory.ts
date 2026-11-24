import { http } from '@/request/http';
import type { PageQuery, PageResult } from '@/api/types';

export interface ShopCategoryRow {
  id?: number | string;
  pid?: number | string;
  cateName?: string;
  sort?: number;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface ShopCategoryQuery extends PageQuery {
  cateName?: string;
  status?: number;
}

export const getShopCategoryListApi = (params: ShopCategoryQuery) => {
  return http.get<PageResult<ShopCategoryRow>>('/client/shopCategory', params);
};

export const getShopCategoryAllApi = () => {
  return http.get<ShopCategoryRow[]>('/client/shopCategory/allCate');
};
