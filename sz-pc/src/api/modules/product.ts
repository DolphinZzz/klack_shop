import { http } from '@/request/http';
import type { PageQuery, PageResult } from '@/api/types';

export interface ProductDetailInfo {
  id: number;
  image: string;
  sliderImage: string;
  productName: string;
  productInfo: string;
  keyword: string;
  cateId: number;
  price: number;
  vipPrice: number;
  otPrice: number;
  isPostage: number;
  expressName: string;
  expressId: number;
  postage: number;
  unitName: string;
  sales: number;
  stock: number;
  isShow: number;
  isHot: number;
  isBest: number;
  isNew: number;
  isSeckill: number;
  isBargain: number;
  giveIntegral: number;
  desc: string;
  createTime: string;
  updateTime: string;
}

export interface ProductDetailSku {
  id: number;
  productId: number;
  sku: string;
  stock: number;
  price: number;
  flashPrice?: number;
  image: string;
  cost: number;
  barCode: string;
  otPrice: number;
  weight: number;
  volume: number;
  isDel: boolean;
  isDefault: boolean;
  attrValue: string;
}

export interface ProductDetailAttrOption {
  attrName?: string;
  optionName?: string;
}

export interface ProductDetailAttr {
  id: number;
  attrName: string;
  optionList: ProductDetailAttrOption[];
}

export interface ProductFlashSaleInfo {
  id: number;
  productId: number;
  productName: string;
  image: string;
  price: number;
  flashPrice: number;
  stock: number;
  sales: number;
  browse: number;
  fictiBrowse: number;
  limitPerUser: number;
  status: number;
  statusText: string;
  canRush: boolean;
  remainingSeconds: number;
  startTime: string;
  endTime: string;
}

export interface ProductDetailData {
  productInfo: ProductDetailInfo;
  productAttr: ProductDetailAttr[];
  productValue: Record<string, ProductDetailSku>;
  flashSale?: ProductFlashSaleInfo | null;
}

export interface ProductListQuery extends PageQuery {
  keyword?: string;
  cid?: string;
  salesOrder?: 'asc' | 'desc' | '';
  priceOrder?: 'asc' | 'desc' | '';
  minPrice?: number;
  maxPrice?: number;
}

export interface ProductListItem {
  id: number;
  image: string;
  storeName?: string;
  productName?: string;
  price: number;
  otPrice?: number;
  sales?: number;
  fictiBrowse?: number;
  positiveRatio?: string;
  replyNum?: number;
  unitName?: string;
  activity?: string;
  stock?: number;
}

export interface ProductCommentQuery extends PageQuery {
}

export interface ProductCommentItem {
  id: number;
  userName?: string;
  userAvatar?: string;
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

export const getProductDetailApi = (id: number | string) => {
  return http.get<ProductDetailData>(`/client/shopProduct/detail/${id}`, undefined, { skipAuth: true });
};

export const getProductListApi = (data: ProductListQuery) => {
  return http.post<PageResult<ProductListItem>>('/client/shopProduct/list', data, { skipAuth: true });
};

export const getProductCommentListApi = (id: number | string, params: ProductCommentQuery) => {
  return http.get<PageResult<ProductCommentItem>>(`/client/shopProduct/comment/${id}`, params, { skipAuth: true });
};
