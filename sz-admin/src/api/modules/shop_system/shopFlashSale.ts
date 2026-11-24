import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type { ShopFlashSaleDetailRow, ShopFlashSaleQuery, ShopFlashSaleRow, ShopFlashSaleUpdateForm } from '@/api/types/shop_system/shopFlashSale';

export const getShopFlashSaleListApi = (params: ShopFlashSaleQuery) => {
  return http.get<IPage<ShopFlashSaleRow>>(ADMIN_MODULE + '/server/shopFlashSale', params);
};

export const getShopFlashSaleDetailApi = (productId: number) => {
  return http.get<ShopFlashSaleDetailRow>(ADMIN_MODULE + `/server/shopFlashSale/detail/${productId}`);
};

export const updateShopFlashSaleApi = (params: ShopFlashSaleUpdateForm) => {
  return http.put(ADMIN_MODULE + '/server/shopFlashSale', params);
};
