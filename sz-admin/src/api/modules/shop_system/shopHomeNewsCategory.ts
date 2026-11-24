import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type {
  ShopHomeNewsCategoryForm,
  ShopHomeNewsCategoryQuery,
  ShopHomeNewsCategoryRow
} from '@/api/types/shop_system/shopHomeNewsCategory';

export const getShopHomeNewsCategoryListApi = (params: ShopHomeNewsCategoryQuery) => {
  return http.get<IPage<ShopHomeNewsCategoryRow>>(ADMIN_MODULE + `/server/shopHomeNewsCategory`, params);
};

export const getShopHomeNewsCategoryAllApi = () => {
  return http.get<ShopHomeNewsCategoryRow[]>(ADMIN_MODULE + `/server/shopHomeNewsCategory/all`);
};

export const createShopHomeNewsCategoryApi = (params: ShopHomeNewsCategoryForm) => {
  return http.post(ADMIN_MODULE + `/server/shopHomeNewsCategory`, params);
};

export const updateShopHomeNewsCategoryApi = (params: ShopHomeNewsCategoryForm) => {
  return http.put(ADMIN_MODULE + `/server/shopHomeNewsCategory`, params);
};

export const removeShopHomeNewsCategoryApi = (params: { ids: (string | number)[] }) => {
  return http.delete(ADMIN_MODULE + `/server/shopHomeNewsCategory`, params);
};

export const getShopHomeNewsCategoryDetailApi = (params: { id: number }) => {
  const { id } = params;
  return http.get<ShopHomeNewsCategoryRow>(ADMIN_MODULE + `/server/shopHomeNewsCategory/${id}`);
};
