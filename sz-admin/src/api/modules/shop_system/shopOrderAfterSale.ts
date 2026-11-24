import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type {
  ShopOrderAfterSaleAuditDTO,
  ShopOrderAfterSaleQuery,
  ShopOrderAfterSaleReceiveDTO,
  ShopOrderAfterSaleRow
} from '@/api/types/shop_system/shopOrderAfterSale';

export const getShopOrderAfterSaleListApi = (params: ShopOrderAfterSaleQuery) => {
  return http.get<IPage<ShopOrderAfterSaleRow>>(ADMIN_MODULE + `/server/shopOrderAfterSale`, params);
};

export const getShopOrderAfterSaleDetailApi = (id: number) => {
  return http.get<ShopOrderAfterSaleRow>(ADMIN_MODULE + `/server/shopOrderAfterSale/detail/${id}`);
};

export const auditShopOrderAfterSaleApi = (params: ShopOrderAfterSaleAuditDTO) => {
  return http.post(ADMIN_MODULE + `/server/shopOrderAfterSale/audit`, params);
};

export const receiveShopOrderAfterSaleApi = (params: ShopOrderAfterSaleReceiveDTO) => {
  return http.post(ADMIN_MODULE + `/server/shopOrderAfterSale/receive`, params);
};
