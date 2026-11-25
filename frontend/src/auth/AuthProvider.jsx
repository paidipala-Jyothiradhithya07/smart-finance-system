import { useState, useEffect, useCallback, useMemo } from 'react';
import { AuthContext } from './authContext';
import { getStoredAuth, setStoredAuth, clearStoredAuth } from '../utils/storage';
import { apiClient, setUnauthorizedCallback } from '../api/client';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [sessionExpired, setSessionExpired] = useState(false);

  useEffect(() => {
    let isMounted = true;

    const initAuth = async () => {
      const auth = getStoredAuth();
      if (auth?.token) {
        try {
          const response = await apiClient.get('/api/auth/me');
          if (isMounted) {
            setUser(response.data);
          }
        } catch {
          clearStoredAuth();
          if (isMounted) {
            setUser(null);
          }
        }
      } else {
        clearStoredAuth();
        if (isMounted) {
          setUser(null);
        }
      }
      if (isMounted) {
        setLoading(false);
      }
    };

    initAuth();

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    setUnauthorizedCallback(() => {
      setUser(null);
      setSessionExpired(true);
    });

    return () => {
      setUnauthorizedCallback(null);
    };
  }, []);

  const login = useCallback(async (email, password) => {
    const response = await apiClient.post('/api/auth/login', { email, password });
    const { token, expiresInSeconds, user: loggedInUser } = response.data;
    const expiresAt = expiresInSeconds ? Date.now() + expiresInSeconds * 1000 : null;
    setStoredAuth(token, expiresAt);
    setUser(loggedInUser);
    setSessionExpired(false);
    return response.data;
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiClient.post('/api/auth/logout');
    } catch {
      // ignores a failure
    } finally {
      clearStoredAuth();
      setUser(null);
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      setUser,
      loading,
      sessionExpired,
      setSessionExpired,
      login,
      logout,
    }),
    [user, loading, sessionExpired, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export default AuthProvider;
