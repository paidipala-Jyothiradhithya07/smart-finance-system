import axios from 'axios';
import { getStoredAuth, clearStoredAuth } from '../utils/storage';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

let unauthorizedCallback = null;

export function setUnauthorizedCallback(fn) {
  unauthorizedCallback = fn;
}

const PUBLIC_ENDPOINTS = [
  '/api/auth/login',
  '/api/auth/register',
  '/api/meta/currencies',
];

function isPublicUrl(url) {
  if (!url) {
    return false;
  }
  return PUBLIC_ENDPOINTS.some((endpoint) => url.includes(endpoint));
}

apiClient.interceptors.request.use(
  (config) => {
    const url = config.url || '';
    if (!isPublicUrl(url)) {
      const auth = getStoredAuth();
      const token = typeof auth === 'string' ? auth : auth?.token;
      if (token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || '';
      const isLogin = url.includes('/api/auth/login');
      const isRegister = url.includes('/api/auth/register');

      if (!isLogin && !isRegister) {
        clearStoredAuth();
        if (typeof unauthorizedCallback === 'function') {
          unauthorizedCallback();
        }
      }
    }
    return Promise.reject(error);
  }
);

export { apiClient };
export default apiClient;
