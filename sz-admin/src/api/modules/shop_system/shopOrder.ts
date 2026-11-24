import http from '@/api';
import { ADMIN_MODULE } from '@/api/helper/prefix';
import type { IPage } from '@/api/types';
import type {
  ShopOrderQuery,
  ShopOrderRow,
  ShopOrderForm,
  ShopOrderDetail,
  ShopOrderDeliverDTO
} from '@/api/types/shop_system/shopOrder';

/**
 * 查询列表
 * @param params
 * @returns {*}
 */
export const getShopOrderListApi = (params: ShopOrderQuery) => {
  return http.get<IPage<ShopOrderRow>>(ADMIN_MODULE + `/server/shopOrder`, params);
};

/**
 * 添加
 * @param params
 * @returns {*}
 */
export const createShopOrderApi = (params: ShopOrderForm) => {
  return http.post(ADMIN_MODULE + `/server/shopOrder`, params);
};

/**
 * 修改
 * @param params
 * @returns {*}
 */
export const updateShopOrderApi = (params: ShopOrderForm) => {
  return http.put(ADMIN_MODULE + `/server/shopOrder`, params);
};

/**
 * 删除
 * @param params
 * @returns {*}
 */
export const removeShopOrderApi = (params: { ids: (string | number)[] }) => {
  return http.delete(ADMIN_MODULE + `/server/shopOrder`, params);
};

/**
 * 获取详情
 * @param params
 * @returns {*}
 */
export const getShopOrderDetailApi = (params: { id: number }) => {
  const { id } = params;
  return http.get<ShopOrderRow>(ADMIN_MODULE + `/server/shopOrder/${id}`);
};

/**
 * 根据订单号获取订单详情
 * @param params
 * @returns {*}
 */
export const getShopOrderDetailByOrderNoApi = (params: { orderNo: string }) => {
  const { orderNo } = params;
  return http.get<ShopOrderDetail>(ADMIN_MODULE + `/server/shopOrder/detail/${orderNo}`);
};

/**
 * 订单发货
 * @param params
 * @returns {*}
 */
export const deliverShopOrderApi = (params: ShopOrderDeliverDTO) => {
  return http.post(ADMIN_MODULE + `/server/shopOrder/deliver`, params);
};
