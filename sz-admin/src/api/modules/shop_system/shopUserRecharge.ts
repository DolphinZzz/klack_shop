import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type { ShopUserRechargeQuery, ShopUserRechargeRow, ShopUserRechargeForm } from '@/api/types/shop_system/shopUserRecharge';

/**
 * 查询列表
 * @param params
 * @returns {*}
 */
export const getShopUserRechargeListApi = (params: ShopUserRechargeQuery) => {
  return http.get<IPage<ShopUserRechargeRow>>(ADMIN_MODULE + `/server/shopUserRecharge`, params);
};

/**
 * 添加
 * @param params
 * @returns {*}
 */
export const createShopUserRechargeApi = (params: ShopUserRechargeForm) => {
  return http.post(ADMIN_MODULE + `/server/shopUserRecharge`, params);
};

/**
 * 修改
 * @param params
 * @returns {*}
 */
export const updateShopUserRechargeApi = (params: ShopUserRechargeForm) => {
  return http.put(ADMIN_MODULE + `/server/shopUserRecharge`, params);
};

/**
 * 删除
 * @param params
 * @returns {*}
 */
export const removeShopUserRechargeApi = (params: { ids: (string | number)[] }) => {
  return http.delete(ADMIN_MODULE + `/server/shopUserRecharge`, params);
};

/**
 * 获取详情
 * @param params
 * @returns {*}
 */
export const getShopUserRechargeDetailApi = (params: { id: number }) => {
  const { id } = params;
  return http.get<ShopUserRechargeRow>(ADMIN_MODULE + `/server/shopUserRecharge/${id}`);
};

/**
 * 导出excel
 * @param params
 * @returns {*}
 */
export const exportShopUserRechargeExcelApi = (params: ShopUserRechargeQuery) => {
  return http.download(ADMIN_MODULE + `/server/shopUserRecharge/export`, params);
};
