import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { getErrorMessage } from '../utils/errors';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [capsLockActive, setCapsLockActive] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const isExpired = location.state?.expired === true;
  const notice = location.state?.notice || location.state?.message || location.state?.successMessage;

  // Real-time email format validation indicator
  const isValidEmailFormat = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const handleKeyDown = (e) => {
    if (typeof e.getModifierState === 'function') {
      setCapsLockActive(e.getModifierState('CapsLock'));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSubmitting(true);

    try {
      await login(email, password);
      const destination = location.state?.from || '/dashboard';
      navigate(destination, { replace: true });
    } catch (err) {
      setErrorMessage(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-5">
          <div className="premium-card p-4 p-md-5 animate-fade-in">
            <div className="text-center mb-4">
              <div className="brand-badge mb-3" style={{ width: '48px', height: '48px', margin: '0 auto' }}>
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="12" y1="1" x2="12" y2="23"></line>
                  <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
                </svg>
              </div>
              <h1 className="h3 fw-bold text-dark mb-1">Welcome Back</h1>
              <p className="text-muted small">Access your personal finance dashboard</p>
            </div>

            {isExpired && (
              <div className="alert alert-warning d-flex align-items-center gap-2 mb-3 animate-fade-in" role="alert">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                <span>Your session has expired. Please log in again.</span>
              </div>
            )}

            {notice && !isExpired && !errorMessage && (
              <div className="alert alert-success d-flex align-items-center gap-2 mb-3 animate-fade-in" role="alert">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <span>{notice}</span>
              </div>
            )}

            {errorMessage && (
              <div className="alert alert-danger d-flex align-items-center gap-2 mb-3 animate-fade-in" role="alert">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                <span>{errorMessage}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} noValidate>
              <div className="mb-3">
                <div className="d-flex justify-content-between align-items-center">
                  <label htmlFor="login-email-input" className="form-label">
                    Email address
                  </label>
                  {email && isValidEmailFormat && (
                    <span className="badge bg-success bg-opacity-10 text-success small fw-normal">
                      ✓ Valid format
                    </span>
                  )}
                </div>
                <input
                  id="login-email-input"
                  type="email"
                  className="form-control"
                  data-testid="login-email"
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                />
              </div>

              <div className="mb-3">
                <div className="d-flex justify-content-between align-items-center">
                  <label htmlFor="login-password-input" className="form-label">
                    Password
                  </label>
                  {capsLockActive && (
                    <span className="badge bg-warning bg-opacity-25 text-dark small fw-normal">
                      ▲ Caps Lock is ON
                    </span>
                  )}
                </div>
                <div className="input-with-action">
                  <input
                    id="login-password-input"
                    type={showPassword ? 'text' : 'password'}
                    className="form-control pe-5"
                    data-testid="login-password"
                    placeholder="Enter your password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    onKeyDown={handleKeyDown}
                    onKeyUp={handleKeyDown}
                    required
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    className="input-action-btn"
                    onClick={() => setShowPassword((prev) => !prev)}
                    title={showPassword ? 'Hide password' : 'Show password'}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? (
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                        <line x1="1" y1="1" x2="23" y2="23"></line>
                      </svg>
                    ) : (
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                      </svg>
                    )}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                className="btn btn-primary w-100 py-2 d-flex align-items-center justify-content-center gap-2 mt-4"
                data-testid="login-submit"
                disabled={submitting}
              >
                {submitting && (
                  <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                )}
                {submitting ? 'Signing in...' : 'Sign In'}
              </button>
            </form>

            <div className="mt-4 pt-3 border-top text-center">
              <span className="text-muted small">Don't have an account yet? </span>
              <Link to="/register" className="fw-semibold text-decoration-none">
                Create Account
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
