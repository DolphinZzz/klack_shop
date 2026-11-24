import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type { ShopProductQuery, ShopProductRow, ShopProductForm } from '@/api/types/shop_system/shopProduct';

/**
 * 查询列表
 * @param params
 * @returns {*}
 */
export const getShopProductListApi = (params: ShopProductQuery) => {
  return http.get<IPage<ShopProductRow>>(ADMIN_MODULE + `/server/shopProduct`, params);
};

/**
 * 添加
 * @param params
 * @returns {*}
 */
export const createShopProductApi = (params: ShopProductForm) => {
  return http.post(ADMIN_MODULE + `/server/shopProduct`, params);
};

/**
 * 修改
 * @param params
 * @returns {*}
 */
export const updateShopProductApi = (params: ShopProductForm) => {
  return http.put(ADMIN_MODULE + `/server/shopProduct`, params);
};

/**
 * 删除
 * @param params
 * @returns {*}
 */
export const removeShopProductApi = (params: { ids: (string | number)[] }) => {
  return http.delete(ADMIN_MODULE + `/server/shopProduct`, params);
};

/**
 * 获取详情
 * @param params
 * @returns {*}
 */
export const getShopProductDetailApi = (params: { productId: number }) => {
  const { productId } = params;
  return http.get<ShopProductRow>(ADMIN_MODULE + `/server/shopProduct/detail/${productId}`);
};

/**
 * 导出excel
 * @param params
 * @returns {*}
 */
export const exportShopProductExcelApi = (params: ShopProductQuery) => {
  return http.download(ADMIN_MODULE + `/server/shopProduct/export`, params);
};
