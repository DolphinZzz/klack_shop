import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type { ShopUserQuery, ShopUserRow, ShopUserForm } from '@/api/types/shop_system/shopUser';
import type { UploadRawFile } from 'element-plus/es/components/upload/src/upload';
import type { AxiosRequestConfig } from 'axios';

/**
 * 查询列表
 * @param params
 * @returns {*}
 */
export const getShopUserListApi = (params: ShopUserQuery) => {
  return http.get<IPage<ShopUserRow>>(ADMIN_MODULE + `/server/shopUser`, params);
};

/**
 * 添加
 * @param params
 * @returns {*}
 */
export const createShopUserApi = (params: ShopUserForm) => {
  return http.post(ADMIN_MODULE + `/server/shopUser`, params);
};

/**
 * 修改
 * @param params
 * @returns {*}
 */
export const updateShopUserApi = (params: ShopUserForm) => {
  return http.put(ADMIN_MODULE + `/server/shopUser`, params);
};

/**
 * 删除
 * @param params
 * @returns {*}
 */
export const removeShopUserApi = (params: { ids: (string | number)[] }) => {
  return http.delete(ADMIN_MODULE + `/server/shopUser`, params);
};

/**
 * 获取详情
 * @param params
 * @returns {*}
 */
export const getShopUserDetailApi = (params: { id: number }) => {
  const { id } = params;
  return http.get<ShopUserRow>(ADMIN_MODULE + `/server/shopUser/${id}`);
};

/**
 * 导入excel
 * @param params
 */
export const importShopUserExcelApi = (params: UploadRawFile, config?: AxiosRequestConfig<any> | undefined) => {
  return http.upload(ADMIN_MODULE + `/server/shopUser/import`, params, config);
};

/**
 * 导出excel
 * @param params
 * @returns {*}
 */
export const exportShopUserExcelApi = (params: ShopUserQuery) => {
  return http.download(ADMIN_MODULE + `/server/shopUser/export`, params);
};
