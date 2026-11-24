import { http } from '@/request/http';
import type { PageQuery, PageResult } from '@/api/types';

export interface CouponCenterQuery extends PageQuery {
  name?: string;
}

export interface CouponCenterItem {
  id: number;
  name?: string;
  money?: string | number;
  total?: number;
  lastTotal?: number;
  type?: number;
  cateKey?: string;
  minMoney?: string | number;
  couponStartTime?: string;
  couponEndTime?: string;
  useStartTime?: string;
  useEndTime?: string;
  couponUserId?: number;
  received?: number;
  productNames?: string;
  productImage?: string;
}

export const getCouponCenterPageApi = (params: CouponCenterQuery) => {
  return http.get<PageResult<CouponCenterItem>>('/client/shopProductCoupon/list', params);
};

export const grabCouponApi = (couponId: number) => {
  return http.post<string>(`/client/shopProductCoupon/grab/${couponId}`);
};
