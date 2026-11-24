import { http } from '@/request/http';
import type { PageQuery, PageResult } from '@/api/types';

export interface UserAddress {
  id: number;
  uid?: number;
  realName: string;
  phone: string;
  provinceId?: number;
  province?: string;
  cityId?: string;
  city?: string;
  districtId?: string | number;
  district?: string;
  detail?: string;
  postCode?: string;
  isDefault?: number;
}

export interface UserAddressListQuery extends PageQuery {}

export const getUserAddressListApi = (params?: UserAddressListQuery) => {
  return http.get<PageResult<UserAddress>>('/client/shopUserAddress/address/list', params);
};

export interface UserAddressCreateParams {
  uid?: number;
  realName: string;
  phone: string;
  provinceId: number;
  province: string;
  cityId: string;
  city: string;
  districtId: string;
  district: string;
  detail: string;
  postCode?: string;
  isDefault?: number;
}

export const createUserAddressApi = (data: UserAddressCreateParams) => {
  return http.post<void>('/client/shopUserAddress/address/add', data);
};

export interface UserAddressUpdateParams {
  id: number;
  uid?: number;
  realName: string;
  phone: string;
  provinceId: number;
  province: string;
  cityId: string;
  city: string;
  districtId: string;
  district: string;
  detail: string;
  postCode?: string;
  isDefault?: number;
}

export const updateUserAddressApi = (data: UserAddressUpdateParams) => {
  return http.put<void>('/client/shopUserAddress/address/edit', data);
};

export const deleteUserAddressApi = (ids: number[]) => {
  return http.delete<void>('/client/shopUserAddress/address/del', { ids });
};

export interface CityListQuery {
  parentId?: number;
  regionType: number;
}

export interface CityItem {
  id: number;
  cityId: number;
  parentId: number;
  regionType: number;
  name: string;
}

export const getCityListApi = (params: CityListQuery) => {
  return http.get<CityItem[]>('/client/city/list', params);
};
