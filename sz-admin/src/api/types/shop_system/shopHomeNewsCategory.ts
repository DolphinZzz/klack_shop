import type { IPageQuery } from '@/api/types';

export type ShopHomeNewsCategoryQuery = IPageQuery & {
  categoryName?: string;
  status?: number;
};

export type ShopHomeNewsCategoryForm = {
  id?: number;
  categoryName?: string;
  sort?: number;
  status?: number;
  remark?: string;
};

export type ShopHomeNewsCategoryRow = {
  id?: number;
  categoryName?: string;
  sort?: number;
  status?: number;
  remark?: string;
  createTime?: string;
};
