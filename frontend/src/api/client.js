import axios from 'axios';
import { getStoredAuth, clearStoredAuth } from '../utils/storage';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

let onUnauthorizedCallback = null;

export function setUnauthorizedCallback(callback) {
  onUnauthorizedCallback = callback;
}

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    const url = config.url || '';
    const method = (config.method || 'get').toLowerCase();

    const isLogin = url.includes('/api/auth/login');
    const isRegister = url.includes('/api/auth/register');
    const isCurrencies = url.includes('/api/meta/currencies') && method === 'get';

    if (!isLogin && !isRegister && !isCurrencies) {
      const auth = getStoredAuth();
      if (auth?.token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${auth.token}`;
      }
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || '';
      const isLogin = url.includes('/api/auth/login');
      const isRegister = url.includes('/api/auth/register');
      const isCurrencies = url.includes('/api/meta/currencies');

      // Only handle 401 for protected calls
      if (!isLogin && !isRegister && !isCurrencies) {
        clearStoredAuth();
        if (onUnauthorizedCallback) {
          onUnauthorizedCallback();
        }
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
