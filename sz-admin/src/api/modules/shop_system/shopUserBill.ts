import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type { ShopUserBillQuery, ShopUserBillRow, ShopUserBillForm } from '@/api/types/shop_system/shopUserBill';

/**
 * 查询列表
 * @param params
 * @returns {*}
 */
export const getShopUserBillListApi = (params: ShopUserBillQuery) => {
  return http.get<IPage<ShopUserBillRow>>(ADMIN_MODULE + `/server/shopUserBill`, params);
};

/**
 * 添加
 * @param params
 * @returns {*}
 */
export const createShopUserBillApi = (params: ShopUserBillForm) => {
  return http.post(ADMIN_MODULE + `/server/shopUserBill`, params);
};

/**
 * 修改
 * @param params
 * @returns {*}
 */
export const updateShopUserBillApi = (params: ShopUserBillForm) => {
  return http.put(ADMIN_MODULE + `/server/shopUserBill`, params);
};

/**
 * 删除
 * @param params
 * @returns {*}
 */
export const removeShopUserBillApi = (params: { ids: (string | number)[] }) => {
  return http.delete(ADMIN_MODULE + `/server/shopUserBill`, params);
};

/**
 * 获取详情
 * @param params
 * @returns {*}
 */
export const getShopUserBillDetailApi = (params: { id: number }) => {
  const { id } = params;
  return http.get<ShopUserBillRow>(ADMIN_MODULE + `/server/shopUserBill/${id}`);
};

/**
 * 导出excel
 * @param params
 * @returns {*}
 */
export const exportShopUserBillExcelApi = (params: ShopUserBillQuery) => {
  return http.download(ADMIN_MODULE + `/server/shopUserBill/export`, params);
};
