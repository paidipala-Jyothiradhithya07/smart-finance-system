const TOKEN_KEY = 'token';
const EXPIRES_AT_KEY = 'expiresAt';

export function getStoredAuth() {
  try {
    const token = sessionStorage.getItem(TOKEN_KEY);
    if (!token) {
      return null;
    }

    try {
      const parsed = JSON.parse(token);
      if (parsed && typeof parsed === 'object' && parsed.token) {
        return {
          token: parsed.token,
          expiresAt: parsed.expiresAt ? Number(parsed.expiresAt) : null,
        };
      }
    } catch {
      // Plain string token
    }

    const expiresAt = sessionStorage.getItem(EXPIRES_AT_KEY);
    return {
      token,
      expiresAt: expiresAt ? Number(expiresAt) : null,
    };
  } catch {
    return null;
  }
}

export function setStoredAuth(token, expiresAt) {
  try {
    sessionStorage.setItem(TOKEN_KEY, token);
    if (expiresAt) {
      sessionStorage.setItem(EXPIRES_AT_KEY, String(expiresAt));
    } else {
      sessionStorage.removeItem(EXPIRES_AT_KEY);
    }
  } catch {
    // Fallback if sessionStorage is disabled
  }
}

export function clearStoredAuth() {
  try {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(EXPIRES_AT_KEY);
    sessionStorage.removeItem('auth');
  } catch {
    // Fallback
  }
}

export function isTokenExpired(expiresAt) {
  if (!expiresAt) {
    return false;
  }
  return Date.now() >= expiresAt;
}
