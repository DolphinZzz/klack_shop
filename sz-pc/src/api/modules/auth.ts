import { http } from '@/request/http';

export interface LbUserLoginParams {
  accountOrPhone: string;
  password: string;
}

export interface LbUserRegisterParams {
  account: string;
  password: string;
  phone: string;
  realName?: string;
  nickname?: string;
}

export interface LbUserLoginInfo {
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

export const lbUserLoginApi = (params: LbUserLoginParams) => {
  return http.post<LbUserLoginInfo>('/client/lb/auth/login', params, { skipAuth: true });
};

export const lbUserRegisterApi = (params: LbUserRegisterParams) => {
  return http.post<void>('/client/lb/auth/register', params, { skipAuth: true });
};

export const lbUserLogoutApi = () => {
  return http.post<void>('/client/lb/auth/logout');
};
