import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import apiClient from '../api/client';
import { getErrorMessage } from '../utils/errors';

export default function Register() {
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [currency, setCurrency] = useState('');
  const [currencies, setCurrencies] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Load currencies on mount
  React.useEffect(() => {
    apiClient.get('/api/meta/currencies')
      .then(res => {
        setCurrencies(res.data.currencies || []);
        setCurrency(res.data.defaultCurrency || '');
      })
      .catch(() => setCurrencies([]));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await apiClient.post('/api/auth/register', { name, email, password, currency });
      // On success, go to login with a notice (not implemented notice handling here)
      navigate('/login');
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container mt-5" data-testid="register-page">
      <h2>Register</h2>
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      <form onSubmit={handleSubmit}>
        <div className="mb-3">
          <label className="form-label" htmlFor="name">Name</label>
          <input type="text" className="form-control" id="name" value={name} onChange={e => setName(e.target.value)} required />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="email">Email</label>
          <input type="email" className="form-control" id="email" value={email} onChange={e => setEmail(e.target.value)} required />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="password">Password</label>
          <input type="password" className="form-control" id="password" value={password} onChange={e => setPassword(e.target.value)} required />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="currency">Currency</label>
          <select className="form-select" id="currency" value={currency} onChange={e => setCurrency(e.target.value)} required>
            {currencies.map(c => (
              <option key={c.code} value={c.code}>{c.code} - {c.name}</option>
            ))}
          </select>
        </div>
        <button type="submit" className="btn btn-primary" disabled={loading} data-testid="register-submit">
          {loading ? 'Registering…' : 'Register'}
        </button>
        <p className="mt-3">
          Already have an account? <Link to="/login" data-testid="register-login-link">Login</Link>
        </p>
      </form>
    </div>
  );
}
