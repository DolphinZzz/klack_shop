import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { clearToken, getToken, setToken } from '@/utils/token';

export interface LoginUserInfo {
  uid?: number;
  account?: string;
  username?: string;
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
}

export const useAuthStore = defineStore(
  'auth',
  () => {
    const token = ref(getToken());
    const userInfo = ref<LoginUserInfo>({});

    const isLogin = computed(() => Boolean(token.value));

    const updateToken = (value: string) => {
      token.value = value;
      if (value) {
        setToken(value);
      } else {
        clearToken();
      }
    };

    const updateUserInfo = (value: LoginUserInfo) => {
      userInfo.value = {
        ...userInfo.value,
        ...value
      };
    };

    const logout = () => {
      token.value = '';
      userInfo.value = {};
      clearToken();
    };

    return {
      token,
      userInfo,
      isLogin,
      updateToken,
      updateUserInfo,
      logout
    };
  },
  {
    persist: {
      key: 'sz-pc-auth',
      paths: ['token', 'userInfo']
    }
  }
);
