import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type {
  ShopHomeBannerQuery,
  ShopHomeBannerRow,
  ShopHomeBannerForm
} from '@/api/types/shop_system/shopHomeBanner';

/**
* 查询列表
* @param params
* @returns {*}
*/
export const getShopHomeBannerListApi = (params: ShopHomeBannerQuery) => {
  return http.get<IPage<ShopHomeBannerRow>>(ADMIN_MODULE + `/server/shopHomeBanner`, params);
};

/**
* 添加
* @param params
* @returns {*}
*/
export const createShopHomeBannerApi = (params: ShopHomeBannerForm) => {
  return http.post(ADMIN_MODULE + `/server/shopHomeBanner`, params);
};

/**
* 修改
* @param params
* @returns {*}
*/
export const updateShopHomeBannerApi = (params: ShopHomeBannerForm) => {
  return http.put(ADMIN_MODULE + `/server/shopHomeBanner`, params);
};

/**
* 删除
* @param params
* @returns {*}
*/
export const removeShopHomeBannerApi = (params: { ids: (string | number)[] }) => {
 return http.delete(ADMIN_MODULE + `/server/shopHomeBanner`, params);
};

/**
* 获取详情
* @param params
* @returns {*}
*/
export const getShopHomeBannerDetailApi = (params: { id: number }) => {
  const { id } = params;
  return http.get<ShopHomeBannerRow>(ADMIN_MODULE + `/server/shopHomeBanner/${id}`);
};

