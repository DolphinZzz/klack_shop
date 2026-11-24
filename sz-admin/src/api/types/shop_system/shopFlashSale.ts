import type { IPageQuery } from '@/api/types';

export type ShopFlashSaleQuery = IPageQuery & {
  productName?: string;
  productShowStatus?: number;
  flashStatus?: number;
};

export type ShopFlashSaleSkuRow = {
  skuId?: number;
  sku?: string;
  attrValue?: string;
  stock?: number;
  price?: number | string;
  flashPrice?: number | string;
  isDefault?: number | boolean;
  image?: string;
};

export type ShopFlashSaleRow = {
  id?: number;
  productId?: number;
  productName?: string;
  image?: string;
  price?: number | string;
  stock?: number;
  browse?: number;
  fictiBrowse?: number;
  sales?: number;
  productShowStatus?: number;
  isSeckill?: number;
  activity?: string;
  flashStatus?: number;
  flashPrice?: number | string;
  limitPerUser?: number;
  reserveSeconds?: number;
  startTime?: string;
  endTime?: string;
  flashActive?: number;
};

export type ShopFlashSaleDetailRow = {
  productId?: number;
  productName?: string;
  image?: string;
  price?: number | string;
  stock?: number;
  browse?: number;
  fictiBrowse?: number;
  sales?: number;
  productShowStatus?: number;
  isSeckill?: number;
  activity?: string;
  flashStatus?: number;
  flashPrice?: number | string;
  limitPerUser?: number;
  reserveSeconds?: number;
  startTime?: string;
  endTime?: string;
  skuList?: ShopFlashSaleSkuRow[];
};

export type ShopFlashSaleUpdateForm = {
  id?: number;
  productId?: number;
  browse?: number;
  fictiBrowse?: number;
  sales?: number;
  productShowStatus?: number;
  isSeckill?: number;
  activity?: string;
  flashStatus?: number;
  flashPrice?: number | string;
  limitPerUser?: number;
  reserveSeconds?: number;
  startTime?: string;
  endTime?: string;
  skuList?: Array<{
    skuId?: number;
    flashPrice?: number | string;
  }>;
};
