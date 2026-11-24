import type { IPageQuery } from '@/api/types';

export type ShopOrderAfterSaleQuery = IPageQuery & {
  serviceNo?: string;
  orderNo?: string;
  afterSaleType?: number;
  status?: number;
};

export type ShopOrderAfterSaleProduct = {
  productId?: number;
  skuId?: number;
  productName?: string;
  image?: string;
  sku?: string;
  price?: string | number;
  cartNum?: number;
};

export type ShopOrderAfterSaleRow = {
  id?: number;
  serviceNo?: string;
  orderId?: number;
  orderNo?: string;
  uid?: number;
  realName?: string;
  userPhone?: string;
  userAddress?: string;
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
  orderInfoList?: ShopOrderAfterSaleProduct[];
};

export type ShopOrderAfterSaleAuditDTO = {
  id?: number;
  approved?: boolean;
  auditRemark?: string;
};

export type ShopOrderAfterSaleReceiveDTO = {
  id?: number;
  receiveRemark?: string;
};
