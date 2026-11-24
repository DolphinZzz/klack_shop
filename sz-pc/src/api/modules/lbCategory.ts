import { http } from '@/request/http';
import type { PageQuery, PageResult } from '@/api/types';

export interface LbCategoryRow {
  id?: number | string;
  pid?: number | string;
  cateName?: string;
  sort?: number;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface LbCategoryQuery extends PageQuery {
  cateName?: string;
  status?: number;
}

export const getLbCategoryListApi = (params: LbCategoryQuery) => {
  return http.get<PageResult<LbCategoryRow>>('/client/shopCategory', params);
};

export const getLbCategoryAllApi = () => {
  return http.get<LbCategoryRow[]>('/client/shopCategory/allCate');
};
