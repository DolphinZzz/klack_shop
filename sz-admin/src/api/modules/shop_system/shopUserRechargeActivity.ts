import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type {
  ShopUserRechargeActivityQuery,
  ShopUserRechargeActivityRow,
  ShopUserRechargeActivityForm
} from '@/api/types/shop_system/shopUserRechargeActivity';

/**
 * 查询列表
 * @param params
 * @returns {*}
 */
export const getShopUserRechargeActivityListApi = (params: ShopUserRechargeActivityQuery) => {
  return http.get<IPage<ShopUserRechargeActivityRow>>(ADMIN_MODULE + `/server/shopUserRechargeActivity`, params);
};

/**
 * 添加
 * @param params
 * @returns {*}
 */
export const createShopUserRechargeActivityApi = (params: ShopUserRechargeActivityForm) => {
  return http.post(ADMIN_MODULE + `/server/shopUserRechargeActivity`, params);
};

/**
 * 修改
 * @param params
 * @returns {*}
 */
export const updateShopUserRechargeActivityApi = (params: ShopUserRechargeActivityForm) => {
  return http.put(ADMIN_MODULE + `/server/shopUserRechargeActivity`, params);
};

/**
 * 删除
 * @param params
 * @returns {*}
 */
export const removeShopUserRechargeActivityApi = (params: { ids: (string | number)[] }) => {
  return http.delete(ADMIN_MODULE + `/server/shopUserRechargeActivity`, params);
};

/**
 * 获取详情
 * @param params
 * @returns {*}
 */
export const getShopUserRechargeActivityDetailApi = (params: { id: number }) => {
  const { id } = params;
  return http.get<ShopUserRechargeActivityRow>(ADMIN_MODULE + `/server/shopUserRechargeActivity/${id}`);
};
