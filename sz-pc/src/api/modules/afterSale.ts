import { http } from '@/request/http';
import type { PageQuery, PageResult } from '@/api/types';
import type { UserOrderProduct } from '@/api/modules/order';

export interface AfterSaleQuery extends PageQuery {
  status?: number;
  keyword?: string;
}

export interface AfterSaleItem {
  id: number;
  serviceNo?: string;
  orderId?: number;
  orderNo?: string;
  afterSaleType?: number;
  afterSaleTypeText?: string;
  status?: number;
  statusText?: string;
  applyAmount?: string | number;
  reason?: string;
  userRemark?: string;
  auditRemark?: string;
  receiveRemark?: string;
  returnExpressName?: string;
  returnExpressNo?: string;
  returnRemark?: string;
  auditTime?: string;
  returnTime?: string;
  receiveTime?: string;
  refundTime?: string;
  createTime?: string;
  canFillReturnExpress?: boolean;
  orderInfoList?: UserOrderProduct[];
}

export interface AfterSaleApplyParams {
  orderNo: string;
  afterSaleType: number;
  applyAmount?: number;
  reason: string;
  userRemark?: string;
}

export interface AfterSaleReturnParams {
  id: number;
  returnExpressName: string;
  returnExpressNo: string;
  returnRemark?: string;
}

export const getAfterSalePageApi = (params: AfterSaleQuery) => {
  return http.get<PageResult<AfterSaleItem>>('/client/shopOrderAfterSale/page', params);
};

export const getAfterSaleDetailApi = (id: number) => {
  return http.get<AfterSaleItem>(`/client/shopOrderAfterSale/detail/${id}`);
};

export const applyAfterSaleApi = (data: AfterSaleApplyParams) => {
  return http.post<void>('/client/shopOrderAfterSale/apply', data);
};

export const submitAfterSaleReturnApi = (data: AfterSaleReturnParams) => {
  return http.post<void>('/client/shopOrderAfterSale/return', data);
};

