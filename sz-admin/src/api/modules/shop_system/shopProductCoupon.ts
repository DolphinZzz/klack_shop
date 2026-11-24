import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type {
  ShopProductCouponQuery,
  ShopProductCouponRow,
  ShopProductCouponForm
} from '@/api/types/shop_system/shopProductCoupon';

/**
* 查询列表
* @param params
* @returns {*}
*/
export const getShopProductCouponListApi = (params: ShopProductCouponQuery) => {
  return http.get<IPage<ShopProductCouponRow>>(ADMIN_MODULE + `/server/shopProductCoupon`, params);
};

/**
* 添加
* @param params
* @returns {*}
*/
export const createShopProductCouponApi = (params: ShopProductCouponForm) => {
  return http.post(ADMIN_MODULE + `/server/shopProductCoupon`, params);
};

/**
* 修改
* @param params
* @returns {*}
*/
export const updateShopProductCouponApi = (params: ShopProductCouponForm) => {
  return http.put(ADMIN_MODULE + `/server/shopProductCoupon`, params);
};

/**
* 删除
* @param params
* @returns {*}
*/
export const removeShopProductCouponApi = (params: { ids: (string | number)[] }) => {
 return http.delete(ADMIN_MODULE + `/server/shopProductCoupon`, params);
};

/**
* 获取详情
* @param params
* @returns {*}
*/
export const getShopProductCouponDetailApi = (params: { id: number }) => {
  const { id } = params;
  return http.get<ShopProductCouponRow>(ADMIN_MODULE + `/server/shopProductCoupon/${id}`);
};

