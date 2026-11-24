import { http } from '@/request/http';
import type { PageQuery, PageResult } from '@/api/types';

export interface FavoriteItem {
  id: number;
  productId: number;
  productName?: string;
  productInfo?: string;
  productImage?: string;
  productPrice?: string | number;
  giveIntegral?: string | number;
  createTime?: string;
  updateTime?: string;
}

export interface FavoriteStatus {
  collected: boolean;
  favoriteId?: number;
}

export interface FavoriteListQuery extends PageQuery {
}

export const createFavoriteApi = (productId: number | string) => {
  return http.post<void>(`/client/shopUserFavorite/create?productId=${productId}`);
};

export const deleteFavoriteApi = (ids: number[]) => {
  return http.delete<void>('/client/shopUserFavorite/delete', { ids });
};

export const deleteFavoriteByProductApi = (productId: number | string) => {
  return http.delete<void>(`/client/shopUserFavorite/product/${productId}`);
};

export const getFavoritePageApi = (params: FavoriteListQuery) => {
  return http.get<PageResult<FavoriteItem>>('/client/shopUserFavorite/page', params);
};

export const getFavoriteStatusApi = (productId: number | string) => {
  return http.get<FavoriteStatus>('/client/shopUserFavorite/status', { productId });
};

export const getFavoriteCountApi = () => {
  return http.get<number>('/client/shopUserFavorite/count');
};
