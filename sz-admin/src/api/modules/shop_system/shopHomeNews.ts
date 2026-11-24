import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type {
  ShopHomeNewsForm,
  ShopHomeNewsQuery,
  ShopHomeNewsRow
} from '@/api/types/shop_system/shopHomeNews';

/**
* 查询列表
* @param params
* @returns {*}
*/
export const getShopHomeNewsListApi = (params: ShopHomeNewsQuery) => {
  return http.get<IPage<ShopHomeNewsRow>>(ADMIN_MODULE + `/server/shopHomeNews`, params);
};

/**
* 添加
* @param params
* @returns {*}
*/
export const createShopHomeNewsApi = (params: ShopHomeNewsForm) => {
  return http.post(ADMIN_MODULE + `/server/shopHomeNews`, params);
};

/**
* 修改
* @param params
* @returns {*}
*/
export const updateShopHomeNewsApi = (params: ShopHomeNewsForm) => {
  return http.put(ADMIN_MODULE + `/server/shopHomeNews`, params);
};

/**
* 删除
* @param params
* @returns {*}
*/
export const removeShopHomeNewsApi = (params: { ids: (string | number)[] }) => {
  return http.delete(ADMIN_MODULE + `/server/shopHomeNews`, params);
};

/**
* 获取详情
* @param params
* @returns {*}
*/
export const getShopHomeNewsDetailApi = (params: { id: number }) => {
  const { id } = params;
  return http.get<ShopHomeNewsRow>(ADMIN_MODULE + `/server/shopHomeNews/${id}`);
};
