import type { IPageQuery } from '@/api/types';

// 查询条件
export type ShopHomeNewsQuery = IPageQuery & {
  title?: string;
  categoryId?: number;
  isTop?: number;
  status?: number;
};

// 编辑form表单
export type ShopHomeNewsForm = {
  id?: number;
  categoryId?: number;
  title?: string;
  tagName?: string;
  coverImage?: string;
  summary?: string;
  content?: string;
  link?: string;
  source?: string;
  author?: string;
  browseCount?: number;
  sort?: number;
  isTop?: number;
  status?: number;
  publishTime?: string;
  startTime?: string;
  endTime?: string;
  remark?: string;
};

// list或detail返回结构
export type ShopHomeNewsRow = {
  id?: number;
  categoryId?: number;
  title?: string;
  tagName?: string;
  coverImage?: string;
  summary?: string;
  content?: string;
  link?: string;
  source?: string;
  author?: string;
  browseCount?: number;
  sort?: number;
  isTop?: number;
  status?: number;
  publishTime?: string;
  startTime?: string;
  endTime?: string;
  remark?: string;
  createTime?: string;
};
