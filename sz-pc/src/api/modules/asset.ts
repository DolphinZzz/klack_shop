import { http } from '@/request/http';
import type { PageQuery, PageResult } from '@/api/types';

export interface UserBalanceSummary {
  nowMoney: string | number;
  rechargeAmount: string | number;
  consumeAmount: string | number;
}

export interface UserBalanceBillQuery extends PageQuery {
  tab?: 'all' | 'expend' | 'recharge' | 'income';
}

export interface UserBalanceBillItem {
  id: number;
  linkId?: string;
  pm?: number;
  title?: string;
  type?: string;
  number?: string | number;
  balance?: string | number;
  mark?: string;
  createTime?: string;
}

export interface RechargeActivityItem {
  id: number;
  activityName?: string;
  price?: string | number;
  givePrice?: string | number;
}

export interface UserRechargeParams {
  from?: string;
  price: string;
  activityId?: string;
}

export interface UserCouponQuery extends PageQuery {
  tab?: 'all' | 'unused' | 'used_expired';
}

export interface UserCouponItem {
  id: number;
  cid: number;
  name?: string;
  money?: string | number;
  minMoney?: string | number;
  couponEndTime?: string;
  useStartTime?: string;
  useEndTime?: string;
  uid?: number;
  type?: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
  useTime?: string;
  useType?: number;
}

export interface UserIntegralSummary {
  currentIntegral: number;
  totalGainIntegral: number;
  totalUseIntegral: number;
  freezeIntegral: number;
}

export interface UserIntegralBillQuery extends PageQuery {}

export interface UserIntegralBillItem {
  id: number;
  linkId?: string;
  pm?: number;
  title?: string;
  type?: string;
  number?: number;
  balance?: number;
  mark?: string;
  createTime?: string;
}

export const getUserBalanceSummaryApi = () => {
  return http.get<UserBalanceSummary>('/client/shopUser/balance/summary');
};

export const getUserBalanceBillApi = (params: UserBalanceBillQuery) => {
  return http.get<PageResult<UserBalanceBillItem>>('/client/shopUser/balance/bill', params);
};

export const getRechargeActivityListApi = () => {
  return http.get<RechargeActivityItem[]>('/client/shopUserRechargeActivity/list');
};

export const userRechargeApi = (data: UserRechargeParams) => {
  return http.post<void>('/client/shopUserRecharge/recharge', data);
};

export const getUserCouponPageApi = (params: UserCouponQuery) => {
  return http.get<PageResult<UserCouponItem>>('/client/shopUser/coupon/list', params);
};

export const getUserIntegralSummaryApi = () => {
  return http.get<UserIntegralSummary>('/client/shopUser/integral/summary');
};

export const getUserIntegralBillApi = (params: UserIntegralBillQuery) => {
  return http.get<PageResult<UserIntegralBillItem>>('/client/shopUser/integral/bill', params);
};
