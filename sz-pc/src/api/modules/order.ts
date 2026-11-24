import { http } from '@/request/http';
import type { PageQuery, PageResult } from '@/api/types';

export interface PreOrderCreateParams {
  shoppingCartIds: number[];
  preOrderType?: string;
}

export interface PreOrderItem {
  id: number;
  productId: number;
  skuId: number;
  productName?: string;
  image?: string;
  sku?: string;
  price?: number | string;
  postage?: number | string;
  cartNum: number;
}

export interface PreOrderCouponItem {
  id: number;
  cid: number;
  name: string;
  money: number | string;
  minMoney?: number | string;
  couponEndTime?: string;
  status?: number;
}

export interface PreOrderDetail {
  orderNo: string;
  addressId: number | null;
  freightTotalFee: string;
  payFee: string;
  isUseIntegral: boolean;
  userIntegral: number;
  isUseCoupon: boolean;
  couponPrice?: string;
  deductionPrice?: string;
  finalTotalFee: string;
  totalNum: number;
  orderType?: number;
  remainingSeconds?: number;
  orderInfoList: PreOrderItem[];
  platCouponUserList: PreOrderCouponItem[];
}

export interface PreOrderRecalculateParams {
  preOrderNo: string;
  addressId?: number;
  isUseIntegral?: boolean;
  platUserCouponId?: number;
  userCouponId?: number;
}

export interface OrderCreateParams {
  addressId: string;
  isUseIntegral?: boolean;
  platUserCouponId?: number;
  preOrderNo: string;
}

export interface OrderCreateResult {
  orderNo: string;
  orderType?: number;
  payPrice: string;
}

export interface FlashSaleReserveParams {
  productId: number;
  skuId: number;
  cartNum: number;
}

export interface FlashSaleReserveResult {
  orderNo: string;
  orderType?: number;
  payPrice: string;
}

export interface OrderPaymentInfo {
  orderNo: string;
  payPrice: string;
  totalNum: number;
  paid: number;
  orderType?: number;
  payType?: string;
  remainingSeconds: number;
  nowMoney: string;
  canPay: boolean;
  enableWxpay: boolean;
  enableAlipay: boolean;
  enableYue: boolean;
}

export interface OrderPaymentParams {
  orderNo: string;
  payType?: string;
  payChannel?: string;
}

export interface UserOrderListQuery extends PageQuery {
  tab?: string;
  keyword?: string;
}

export interface UserOrderProduct {
  productId: number;
  skuId: number;
  productName: string;
  image: string;
  sku: string;
  price: string;
  cartNum: number;
}

export interface UserOrderItem {
  orderNo: string;
  paid: number;
  status: number;
  displayTab: string;
  displayStatus: string;
  orderType?: number;
  orderTypeLabel?: string;
  createTime: string;
  payTime?: string;
  payPrice: string;
  totalNum: number;
  firstProductId?: number;
  canPay: boolean;
  canConfirmReceipt: boolean;
  canEvaluate: boolean;
  canBuyAgain: boolean;
  canDelete: boolean;
  canApplyAfterSale?: boolean;
  afterSaleId?: number;
  afterSaleStatus?: number;
  afterSaleStatusText?: string;
  orderInfoList: UserOrderProduct[];
}

export interface UserOrderStatusLog {
  id?: number;
  oid?: number;
  changeType?: string;
  changeMessage?: string;
  operator?: string;
  createTime?: string;
}

export interface UserOrderDetail extends UserOrderItem {
  realName?: string;
  userPhone?: string;
  userAddress?: string;
  payType?: string;
  freightTotalFee?: string;
  expressNo?: string;
  expressName?: string;
  orderStatusList?: UserOrderStatusLog[];
}

export interface UserOrderEvaluateParams {
  orderNo: string;
  score?: number;
  content: string;
}

export interface UserCommentListQuery extends PageQuery {
  keyword?: string;
}

export interface UserCommentItem {
  id: number;
  orderNo?: string;
  productId: number;
  productName?: string;
  productImage?: string;
  productInfo?: string;
  productPrice?: string | number;
  productScore?: number;
  comment?: string;
  pic?: string;
  createTime?: string;
}

export const createPreOrderApi = (data: PreOrderCreateParams) => {
  return http.post<PreOrderDetail>('/client/shopOrder/pre/order', data);
};

export const reserveFlashSaleApi = (data: FlashSaleReserveParams) => {
  return http.post<FlashSaleReserveResult>('/client/shopOrder/flash/reserve', data);
};

export const getPreOrderDetailApi = (orderNo: string) => {
  return http.get<PreOrderDetail>(`/client/shopOrder/pre/order/${orderNo}`);
};

export const recalculatePreOrderApi = (data: PreOrderRecalculateParams) => {
  return http.post<PreOrderDetail>('/client/shopOrder/pre/order/recalculate', data);
};

export const createOrderApi = (data: OrderCreateParams) => {
  return http.post<OrderCreateResult>('/client/shopOrder/order/create', data);
};

export const getOrderPaymentInfoApi = (orderNo: string) => {
  return http.get<OrderPaymentInfo>(`/client/shopOrder/order/payment/${orderNo}`);
};

export const orderPaymentApi = (data: OrderPaymentParams) => {
  return http.post<{ orderNo: string; payType: string; status: boolean }>('/client/shopOrder/order/payment', data);
};

export const getUserOrderListApi = (params: UserOrderListQuery) => {
  return http.get<PageResult<UserOrderItem>>('/client/shopOrder/order/list', params);
};

export const getUserOrderDetailApi = (orderNo: string) => {
  return http.get<UserOrderDetail>(`/client/shopOrder/order/detail/${orderNo}`);
};

export const confirmUserOrderReceiptApi = (orderNo: string) => {
  return http.post<void>(`/client/shopOrder/order/receipt/${orderNo}`);
};

export const submitUserOrderEvaluateApi = (data: UserOrderEvaluateParams) => {
  return http.post<void>('/client/shopOrder/order/evaluate', data);
};

export const getUserCommentListApi = (params: UserCommentListQuery) => {
  return http.get<PageResult<UserCommentItem>>('/client/shopOrder/comment/list', params);
};

export const deleteUserOrderApi = (orderNo: string) => {
  return http.delete<void>(`/client/shopOrder/order/${orderNo}`);
};


