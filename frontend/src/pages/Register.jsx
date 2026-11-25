import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiClient } from '../api/client';
import { getErrorMessage, getFieldErrors } from '../utils/errors';

export default function Register() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [capsLockActive, setCapsLockActive] = useState(false);
  const [currency, setCurrency] = useState('');
  const [currencies, setCurrencies] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const navigate = useNavigate();

  useEffect(() => {
    let isMounted = true;

    const fetchCurrencies = async () => {
      try {
        const response = await apiClient.get('/api/meta/currencies');
        if (isMounted && response.data) {
          const list = response.data.currencies || [];
          setCurrencies(list);
          if (response.data.defaultCurrency) {
            setCurrency(response.data.defaultCurrency);
          } else if (list.length > 0) {
            setCurrency(list[0].code);
          }
        }
      } catch {
        // Currencies will remain empty if fetch fails
      }
    };

    fetchCurrencies();

    return () => {
      isMounted = false;
    };
  }, []);

  // Interactive password rule checks
  const hasValidLength = password.length >= 8 && password.length <= 72;
  const hasLetter = /[a-zA-Z]/.test(password);
  const hasDigit = /\d/.test(password);
  const hasSpecial = /[^a-zA-Z0-9]/.test(password);

  // Dynamic strength calculation
  let strengthScore = 0;
  if (password.length >= 8) strengthScore += 1;
  if (hasLetter) strengthScore += 1;
  if (hasDigit) strengthScore += 1;
  if (hasSpecial || password.length >= 12) strengthScore += 1;

  let strengthLabel = 'Weak';
  let strengthClass = 'weak';
  if (strengthScore === 2) {
    strengthLabel = 'Fair';
    strengthClass = 'fair';
  } else if (strengthScore === 3) {
    strengthLabel = 'Good';
    strengthClass = 'good';
  } else if (strengthScore >= 4) {
    strengthLabel = 'Strong';
    strengthClass = 'strong';
  }

  // Selected currency object
  const selectedCurrencyObj = currencies.find((c) => c.code === currency);

  const handleKeyDown = (e) => {
    if (typeof e.getModifierState === 'function') {
      setCapsLockActive(e.getModifierState('CapsLock'));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setFieldErrors({});
    setSubmitting(true);

    try {
      const payload = {
        name,
        email,
        password,
      };
      if (currency) {
        payload.currency = currency;
      }

      await apiClient.post('/api/auth/register', payload);
      navigate('/login', {
        state: { notice: 'Registration successful! Please log in.' },
      });
    } catch (err) {
      setErrorMessage(getErrorMessage(err));
      setFieldErrors(getFieldErrors(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-6">
          <div className="premium-card p-4 p-md-5 animate-fade-in">
            <div className="text-center mb-4">
              <div className="brand-badge mb-3" style={{ width: '48px', height: '48px', margin: '0 auto' }}>
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                  <circle cx="8.5" cy="7.5" r="4"></circle>
                  <line x1="20" y1="8" x2="20" y2="14"></line>
                  <line x1="23" y1="11" x2="17" y2="11"></line>
                </svg>
              </div>
              <h1 className="h3 fw-bold text-dark mb-1">Create an Account</h1>
              <p className="text-muted small">Start tracking your income, expenses, and savings</p>
            </div>

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
                <label htmlFor="register-name-input" className="form-label">
                  Full Name
                </label>
                <input
                  id="register-name-input"
                  type="text"
                  className={`form-control ${fieldErrors.name ? 'is-invalid' : ''}`}
                  placeholder="John Doe"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  autoComplete="name"
                />
                {fieldErrors.name && (
                  <div className="invalid-feedback d-block">
                    {fieldErrors.name}
                  </div>
                )}
              </div>

              <div className="mb-3">
                <label htmlFor="register-email-input" className="form-label">
                  Email Address
                </label>
                <input
                  id="register-email-input"
                  type="email"
                  className={`form-control ${fieldErrors.email ? 'is-invalid' : ''}`}
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                />
                {fieldErrors.email && (
                  <div className="invalid-feedback d-block">
                    {fieldErrors.email}
                  </div>
                )}
              </div>

              <div className="mb-3">
                <div className="d-flex justify-content-between align-items-center">
                  <label htmlFor="register-password-input" className="form-label">
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
                    id="register-password-input"
                    type={showPassword ? 'text' : 'password'}
                    className={`form-control pe-5 ${fieldErrors.password ? 'is-invalid' : ''}`}
                    placeholder="Create a strong password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    onKeyDown={handleKeyDown}
                    onKeyUp={handleKeyDown}
                    required
                    autoComplete="new-password"
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

                <div className="form-text mt-1 text-muted small">
                  Password must be 8 to 72 characters with at least one letter and one digit.
                </div>

                {password && (
                  <div className="mt-2">
                    <div className="d-flex justify-content-between align-items-center small">
                      <span className="text-muted">Strength:</span>
                      <span className={`fw-semibold text-${strengthClass === 'strong' ? 'success' : strengthClass === 'good' ? 'primary' : strengthClass === 'fair' ? 'warning' : 'danger'}`}>
                        {strengthLabel}
                      </span>
                    </div>
                    <div className="strength-bar-track">
                      <div className={`strength-bar-fill ${strengthClass}`}></div>
                    </div>

                    <div className="password-check-list mt-2">
                      <div className={`password-check-item ${hasValidLength ? 'valid' : ''}`}>
                        <span className="password-check-icon">
                          {hasValidLength ? '✓' : '○'}
                        </span>
                        <span>8 to 72 characters</span>
                      </div>
                      <div className={`password-check-item ${hasLetter ? 'valid' : ''}`}>
                        <span className="password-check-icon">
                          {hasLetter ? '✓' : '○'}
                        </span>
                        <span>Contains at least one letter</span>
                      </div>
                      <div className={`password-check-item ${hasDigit ? 'valid' : ''}`}>
                        <span className="password-check-icon">
                          {hasDigit ? '✓' : '○'}
                        </span>
                        <span>Contains at least one digit</span>
                      </div>
                    </div>
                  </div>
                )}

                {fieldErrors.password && (
                  <div className="invalid-feedback d-block">
                    {fieldErrors.password}
                  </div>
                )}
              </div>

              <div className="mb-4">
                <label htmlFor="register-currency-select" className="form-label">
                  Base Currency
                </label>
                <select
                  id="register-currency-select"
                  className={`form-select ${fieldErrors.currency ? 'is-invalid' : ''}`}
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value)}
                >
                  {currencies.map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.code} - {c.name} ({c.symbol})
                    </option>
                  ))}
                </select>

                {/* Dynamic Currency Format Preview Card */}
                {selectedCurrencyObj && (
                  <div className="currency-preview-box animate-fade-in">
                    <div>
                      <div className="fw-semibold text-dark small">
                        {selectedCurrencyObj.name} ({selectedCurrencyObj.code})
                      </div>
                      <div className="text-muted" style={{ fontSize: '0.78rem' }}>
                        Currency Symbol: <strong className="text-primary">{selectedCurrencyObj.symbol}</strong>
                      </div>
                    </div>
                    <span className="badge bg-white text-dark border px-2 py-1 shadow-sm">
                      Sample: {selectedCurrencyObj.symbol} 50,000.00
                    </span>
                  </div>
                )}

                {fieldErrors.currency && (
                  <div className="invalid-feedback d-block">
                    {fieldErrors.currency}
                  </div>
                )}
              </div>

              <button
                type="submit"
                className="btn btn-primary w-100 py-2 d-flex align-items-center justify-content-center gap-2"
                data-testid="register-submit"
                disabled={submitting}
              >
                {submitting && (
                  <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                )}
                {submitting ? 'Registering...' : 'Register'}
              </button>
            </form>

            <div className="mt-4 pt-3 border-top text-center">
              <span className="text-muted small">Already have an account? </span>
              <Link to="/login" className="fw-semibold text-decoration-none">
                Log In
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
