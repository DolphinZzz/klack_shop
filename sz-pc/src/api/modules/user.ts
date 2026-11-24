import { http } from '@/request/http';

export interface UserProfile {
  uid: number;
  account: string;
  realName?: string;
  sex?: number;
  birthday?: string;
  cardId?: string;
  nickname?: string;
  avatar?: string;
  phone?: string;
  nowMoney?: number | string;
  integral?: number;
  experience?: number;
  level?: number;
  userType?: string;
  createTime?: string;
  updateTime?: string;
  status?: number;
}

export interface UserProfileUpdateParams {
  account: string;
  realName?: string;
  sex?: number;
  birthday?: string;
  cardId?: string;
  nickname?: string;
  avatar?: string;
  phone: string;
}

export interface UserPasswordUpdateParams {
  oldPassword: string;
  newPassword: string;
}

export interface UploadResult {
  url: string;
  filename: string;
  eTag: string;
  objectName: string;
  dirTag: string;
  contextType: string;
  size: number;
  fileId: number;
}

export const getUserProfileApi = () => {
  return http.get<UserProfile>('/client/shopUser/profile');
};

export const updateUserProfileApi = (data: UserProfileUpdateParams) => {
  return http.put<void>('/client/shopUser/profile', data);
};

export const updateUserPasswordApi = (data: UserPasswordUpdateParams) => {
  return http.put<void>('/client/shopUser/password', data);
};

export const uploadUserAvatarApi = (file: File, dirTag = 'avatar') => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('dirTag', dirTag);
  return http.upload<UploadResult>('/client/shopUser/avatar/upload', formData);
};
