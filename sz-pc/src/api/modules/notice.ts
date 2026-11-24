import { http } from '@/request/http';
import type { PageResult } from '@/api/types';

export interface NoticeCategoryItem {
  id: number;
  categoryName: string;
  sort: number;
  status: number;
  remark: string;
}

export interface NoticeItem {
  id: number;
  categoryId: number;
  title: string;
  tagName: string;
  coverImage: string;
  summary: string;
  content: string;
  link: string;
  source: string;
  author: string;
  browseCount: number;
  sort: number;
  isTop: number;
  status: number;
  publishTime: string;
  startTime: string;
  endTime: string;
  remark: string;
}

export const getNoticeCategoryApi = () => {
  return http.get<NoticeCategoryItem[]>('/client/shopHomeNewsCategory/index', undefined, { skipAuth: true });
};

export const getNoticePageApi = (params?: { page?: number; limit?: number; categoryId?: number }) => {
  return http.get<PageResult<NoticeItem>>('/client/shopHomeNews/page', params, { skipAuth: true });
};

export const getNoticeHotApi = (size = 5) => {
  return http.get<NoticeItem[]>('/client/shopHomeNews/hot', { size }, { skipAuth: true });
};

export const getNoticeDetailApi = (id: number) => {
  return http.get<NoticeItem>(`/client/shopHomeNews/${id}`, undefined, { skipAuth: true });
};
