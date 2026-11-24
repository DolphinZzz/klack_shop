import { http } from '@/request/http';

export interface ShopUserLoginParams {
  accountOrPhone: string;
  password: string;
}

export interface ShopUserRegisterParams {
  account: string;
  password: string;
  phone: string;
  realName?: string;
  nickname?: string;
}

export interface ShopUserLoginInfo {
  uid: number;
  account: string;
  nickname?: string;
  realName?: string;
  phone?: string;
  avatar?: string;
  level?: number;
  integral?: number;
  experience?: number;
  nowMoney?: number | string;
  userType?: string;
  createTime?: string;
  accessToken: string;
  expireIn?: number;
}

export const shopUserLoginApi = (params: ShopUserLoginParams) => {
  return http.post<ShopUserLoginInfo>('/client/shop/auth/login', params, { skipAuth: true });
};

export const shopUserRegisterApi = (params: ShopUserRegisterParams) => {
  return http.post<void>('/client/shop/auth/register', params, { skipAuth: true });
};

export const shopUserLogoutApi = () => {
  return http.post<void>('/client/shop/auth/logout');
};
