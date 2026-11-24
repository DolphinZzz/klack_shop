import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';
import { useAuthStore } from '@/stores/modules/auth';
import type { ApiResult } from '@/api/types';

const CODE_SUCCESS = '0000';
const CODE_TOKEN_FAIL = 'C105';

interface RequestConfig extends AxiosRequestConfig {
  skipAuth?: boolean;
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: Number(import.meta.env.VITE_HTTP_TIMEOUT || 60000)
});

service.interceptors.request.use((config: InternalAxiosRequestConfig & RequestConfig) => {
  if (config.skipAuth) return config;
  const authStore = useAuthStore();
  if (authStore.token && config.headers && typeof config.headers.set === 'function') {
    config.headers.set('Authorization', `Bearer ${authStore.token}`);
  }
  return config;
});

service.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') return response;
    const requestConfig = response.config as RequestConfig;
    const body = response.data;

    // 标准响应格式：{ code, message, data }
    if (body && typeof body === 'object' && 'code' in body) {
      const data = body as ApiResult;
      if (data.code === CODE_SUCCESS) return data;
      if (data.code === CODE_TOKEN_FAIL) {
        if (requestConfig.skipAuth) {
          return Promise.reject(data);
        }
        const authStore = useAuthStore();
        authStore.logout();
        router.replace('/login');
        ElMessage.error(data.message || '登录状态失效，请重新登录');
        return Promise.reject(data);
      }
      ElMessage.error(data.message || '请求失败');
      return Promise.reject(data);
    }

    // 兼容响应直接返回业务对象（例如直接返回 accessToken 对象）
    return {
      code: CODE_SUCCESS,
      message: 'success',
      data: body
    } as ApiResult;
  },
  error => {
    ElMessage.error(error?.message || '网络异常');
    return Promise.reject(error);
  }
);

export const http = {
  get<T>(url: string, params?: Record<string, unknown>, config?: RequestConfig) {
    return service.get<any, ApiResult<T>>(url, { params, ...config });
  },
  post<T>(url: string, data?: unknown, config?: RequestConfig) {
    return service.post<any, ApiResult<T>>(url, data, config);
  },
  put<T>(url: string, data?: unknown, config?: RequestConfig) {
    return service.put<any, ApiResult<T>>(url, data, config);
  },
  delete<T>(url: string, data?: unknown, config?: RequestConfig) {
    return service.delete<any, ApiResult<T>>(url, { data, ...config });
  },
  upload<T>(url: string, data: FormData, config?: RequestConfig) {
    return service.post<any, ApiResult<T>>(url, data, config);
  }
};
