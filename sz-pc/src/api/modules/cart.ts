import { http } from '@/request/http';
import type { PageQuery, PageResult } from '@/api/types';

export interface CartCreateParams {
  productId: number;
  skuId: number;
  cartNum: number;
}

export interface CartUpdateParams {
  id: number;
  cartNum: number;
}

export interface CartListQuery extends PageQuery {
  status?: number;
}

export interface CartItem {
  id: number;
  productId: number;
  skuId: number;
  productName: string;
  image: string;
  price: number;
  vipPrice: number;
  sku: string;
  stock: number;
  cartNum: number;
}

export const createCartItemApi = (data: CartCreateParams) => {
  return http.post<void>('/client/shopProductCart/create', data);
};

export const updateCartItemApi = (data: CartUpdateParams) => {
  return http.put<void>('/client/shopProductCart/update', data);
};

export const deleteCartItemApi = (ids: number[]) => {
  return http.delete<void>('/client/shopProductCart/delete', { ids });
};

export const getCartListApi = (params: CartListQuery) => {
  return http.get<PageResult<CartItem>>('/client/shopProductCart/list', params);
};

export const getCartCountApi = () => {
  return http.get<number>('/client/shopProductCart/count');
};
