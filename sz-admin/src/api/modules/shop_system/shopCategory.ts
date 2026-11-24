import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage, IResultData } from '@/api/types';
import type { ShopCategoryQuery, ShopCategoryRow, ShopCategoryForm } from '@/api/types/shop_system/shopCategory';

/**
 * 查询列表
 * @param params
 * @returns {*}
 */
export const getShopCategoryListApi = (params: ShopCategoryQuery) => {
  return http.get<IPage<ShopCategoryRow>>(ADMIN_MODULE + `/server/shopCategory`, params);
};

/**
 * 查询所有分类
 * @param params
 */
export const getShopCategoryAllListApi = () => {
  return http.get<IResultData<ShopCategoryRow[]>>(ADMIN_MODULE + `/server/shopCategory/allCate`);
};

/**
 * 添加
 * @param params
 * @returns {*}
 */
export const createShopCategoryApi = (params: ShopCategoryForm) => {
  return http.post(ADMIN_MODULE + `/server/shopCategory`, params);
};

/**
 * 修改
 * @param params
 * @returns {*}
 */
export const updateShopCategoryApi = (params: ShopCategoryForm) => {
  return http.put(ADMIN_MODULE + `/server/shopCategory`, params);
};

/**
 * 删除
 * @param params
 * @returns {*}
 */
export const removeShopCategoryApi = (params: { ids: (string | number)[] }) => {
  console.log(params);
  return http.delete(ADMIN_MODULE + `/server/shopCategory`, params);
};

/**
 * 获取详情
 * @param params
 * @returns {*}
 */
export const getShopCategoryDetailApi = (params: { id: number }) => {
  const { id } = params;
  return http.get<ShopCategoryRow>(ADMIN_MODULE + `/server/shopCategory/${id}`);
};
