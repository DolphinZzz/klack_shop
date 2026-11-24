import { http } from '@/request/http';

export interface UserFootprintItem {
  id: number;
  productId: number;
  productName?: string;
  productInfo?: string;
  productImage?: string;
  productPrice?: string | number;
  giveIntegral?: string | number;
  createTime?: string;
}

export interface UserFootprintListQuery {
  limit?: number;
}

export const recordUserFootprintApi = (productId: number | string) => {
  return http.post<void>(`/client/shopUserFootprint/record?productId=${productId}`);
};

export const getUserFootprintListApi = (params?: UserFootprintListQuery) => {
  return http.get<UserFootprintItem[]>('/client/shopUserFootprint/list', params);
};
