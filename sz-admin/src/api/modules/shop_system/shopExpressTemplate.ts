import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type {
  ShopExpressTemplateQuery,
  ShopExpressTemplateRow,
  ShopExpressTemplateForm
} from '@/api/types/shop_system/shopExpressTemplate';

/**
* 查询列表
* @param params
* @returns {*}
*/
export const getShopExpressTemplateListApi = (params: ShopExpressTemplateQuery) => {
  return http.get<IPage<ShopExpressTemplateRow>>(ADMIN_MODULE + `/server/shopExpressTemplate`, params);
};

/**
* 添加
* @param params
* @returns {*}
*/
export const createShopExpressTemplateApi = (params: ShopExpressTemplateForm) => {
  return http.post(ADMIN_MODULE + `/server/shopExpressTemplate`, params);
};

/**
* 修改
* @param params
* @returns {*}
*/
export const updateShopExpressTemplateApi = (params: ShopExpressTemplateForm) => {
  return http.put(ADMIN_MODULE + `/server/shopExpressTemplate`, params);
};

/**
* 删除
* @param params
* @returns {*}
*/
export const removeShopExpressTemplateApi = (params: { ids: (string | number)[] }) => {
 return http.delete(ADMIN_MODULE + `/server/shopExpressTemplate`, params);
};

/**
* 获取详情
* @param params
* @returns {*}
*/
export const getShopExpressTemplateDetailApi = (params: { id: number }) => {
  const { id } = params;
  return http.get<ShopExpressTemplateRow>(ADMIN_MODULE + `/server/shopExpressTemplate/${id}`);
};

/**
* 导出excel
* @param params
* @returns {*}
*/
export const exportShopExpressTemplateExcelApi  = (params: ShopExpressTemplateQuery) => {
  return http.download(ADMIN_MODULE + `/server/shopExpressTemplate/export`, params);
};