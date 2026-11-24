import type { IPageQuery } from '@/api/types';

export type ShopOrderQuery = IPageQuery & {
  orderId?: string;
  realName?: string;
  userAddress?: string;
  totalPrice?: number;
  payPrice?: number;
  payType?: string;
  status?: number;
  orderType?: number;
  payTime?: string[];
  payTimeStart?: string;
  payTimeEnd?: string;
  createTime?: string[];
  createTimeStart?: string;
  createTimeEnd?: string;
};

export type ShopOrderForm = {
  id?: number;
  orderId?: string;
  realName?: string;
  userAddress?: string;
  totalPrice?: number;
  payPrice?: number;
  payTime?: string;
  payType?: string;
  status?: number;
  createTime?: string;
  orderType?: number;
};

export type ShopOrderDeliverDTO = {
  orderNo?: string;
  expressTemplateId?: number;
};

export type ShopOrderRow = {
  id?: number;
  orderId?: string;
  realName?: string;
  userAddress?: string;
  totalPrice?: number;
  payPrice?: number;
  payTime?: string;
  payType?: string;
  paid?: number;
  status?: number;
  createTime?: string;
  orderType?: number;
};

export type ShopOrderDetailItem = {
  id?: number;
  uid?: number;
  productId?: number;
  skuId?: number;
  cartNum?: number;
  status?: number;
  sku?: string;
  price?: number | string;
  postage?: number | string;
  productName?: string;
  productImage?: string;
  image?: string;
  imageUrl?: string;
  deliveryNum?: number;
  refundIngNum?: number;
  refundSuccessNum?: number;
  createTime?: string;
  updateTime?: string;
};

export type ShopOrderStatusLog = {
  id?: number;
  oid?: number;
  changeType?: string;
  changeMessage?: string;
  operator?: string;
  createTime?: string;
};

export type ShopOrderDetail = {
  orderNo?: string;
  orderId?: string;
  orderType?: number;
  status?: number;
  payType?: string | number;
  payTime?: string;
  payFee?: number | string;
  finalTotalFee?: number | string;
  freightTotalFee?: number | string;
  totalNum?: number;
  userName?: string;
  realName?: string;
  userPhone?: string;
  userAddress?: string;
  receiverName?: string;
  receiverPhone?: string;
  receiverAddress?: string;
  buyerRemark?: string;
  merchantRemark?: string;
  deliveryRemark?: string;
  expressNo?: string;
  expressCode?: string;
  expressName?: string;
  deliveryId?: string | number;
  platCouponUserList?: Array<Record<string, any>>;
  orderInfoList?: ShopOrderDetailItem[];
  orderStatusList?: ShopOrderStatusLog[];
  [key: string]: any;
};

