import { createContext, useState, useEffect } from 'react';
import apiClient, { setUnauthorizedCallback } from '../api/client';
import { getStoredAuth, setStoredAuth, clearStoredAuth } from '../utils/storage';

export const AuthContext = createContext({
  user: null,
  token: null,
  login: async () => {},
  logout: async () => {},
  loading: true,
});

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  // We only need to wait if there is a stored token to verify
  const [loading, setLoading] = useState(() => Boolean(getStoredAuth()?.token));

  useEffect(() => {
    // A 401 from any protected call ends the session
    setUnauthorizedCallback(() => {
      clearStoredAuth();
      setUser(null);
      setToken(null);
    });

    const stored = getStoredAuth();
    if (!stored?.token) {
      return undefined;
    }

    let cancelled = false;
    (async () => {
      try {
        const response = await apiClient.get('/api/auth/me');
        if (!cancelled) {
          setUser(response.data);
          setToken(stored.token);
        }
      } catch {
        if (!cancelled) {
          clearStoredAuth();
          setUser(null);
          setToken(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  const login = async (email, password) => {
    const response = await apiClient.post('/api/auth/login', { email, password });
    const { token: newToken, expiresInSeconds, user: loggedUser } = response.data;
    const expiresAt = Date.now() + expiresInSeconds * 1000;
    setStoredAuth(JSON.stringify({ token: newToken }), expiresAt);
    setUser(loggedUser);
    setToken(newToken);
    return loggedUser;
  };

  const logout = async () => {
    try {
      await apiClient.post('/api/auth/logout');
    } catch {
      // The server keeps no session, so a failed call doesn't matter: we clear the token below anyway
    }
    clearStoredAuth();
    setUser(null);
    setToken(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
}