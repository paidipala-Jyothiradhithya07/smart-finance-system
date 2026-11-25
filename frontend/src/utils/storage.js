const TOKEN_KEY = 'token';
const EXPIRES_AT_KEY = 'expiresAt';
const AUTH_KEY = 'auth';

export function getStoredAuth() {
  try {
    let token = sessionStorage.getItem(TOKEN_KEY);
    let expiresAt = sessionStorage.getItem(EXPIRES_AT_KEY);

    if (!token) {
      const authRaw = sessionStorage.getItem(AUTH_KEY);
      if (authRaw) {
        try {
          const parsed = JSON.parse(authRaw);
          if (parsed && typeof parsed === 'object') {
            token = parsed.token;
            expiresAt = parsed.expiresAt;
          }
        } catch {
          // ignore invalid JSON
        }
      }
    } else {
      try {
        const parsed = JSON.parse(token);
        if (parsed && typeof parsed === 'object' && parsed.token) {
          token = parsed.token;
          if (parsed.expiresAt) {
            expiresAt = parsed.expiresAt;
          }
        }
      } catch {
        // Plain string token
      }
    }

    if (!token) {
      return null;
    }

    const expNum = expiresAt ? Number(expiresAt) : null;
    if (expNum && !Number.isNaN(expNum)) {
      if (Date.now() >= expNum) {
        clearStoredAuth();
        return null;
      }
    }

    return {
      token,
      expiresAt: expNum,
    };
  } catch {
    clearStoredAuth();
    return null;
  }
}

export function setStoredAuth(token, expiresAt) {
  try {
    let tokenValue = token;
    let expiresAtValue = expiresAt;

    if (token && typeof token === 'object') {
      tokenValue = token.token;
      expiresAtValue = token.expiresAt ?? expiresAt;
    }

    if (!tokenValue) {
      clearStoredAuth();
      return;
    }

    sessionStorage.setItem(TOKEN_KEY, String(tokenValue));

    if (expiresAtValue !== undefined && expiresAtValue !== null) {
      sessionStorage.setItem(EXPIRES_AT_KEY, String(expiresAtValue));
    } else {
      sessionStorage.removeItem(EXPIRES_AT_KEY);
    }

    sessionStorage.setItem(
      AUTH_KEY,
      JSON.stringify({
        token: tokenValue,
        expiresAt: expiresAtValue ? Number(expiresAtValue) : null,
      })
    );
  } catch {
    // sessionStorage not available
  }
}

export function clearStoredAuth() {
  try {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(EXPIRES_AT_KEY);
    sessionStorage.removeItem(AUTH_KEY);
  } catch {
    // ignore
  }
}
