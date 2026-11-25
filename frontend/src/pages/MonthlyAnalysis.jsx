import { useState, useEffect } from 'react';
import { financeApi } from '../api/financeApi';
import { formatMoney } from '../utils/format';
import { useAuth } from '../auth/useAuth';

export default function MonthlyAnalysis() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [range, setRange] = useState({ from: '', to: '' });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await financeApi.monthlySummary(range.from, range.to);
      setData(response.data);
    } catch (err) {
      setError('Failed to load monthly summary');
    } finally {
      setLoading(false);
    }
  };

  const handleFilter = (e) => {
    e.preventDefault();
    loadData();
  };

  return (
    <div className="container py-4 animate-fade-in">
      <h1 className="h3 fw-bold mb-4">Monthly Financial Analysis</h1>

      <div className="premium-card p-4 mb-4">
        <form onSubmit={handleFilter} className="row g-3 align-items-end">
          <div className="col-md-3">
            <label className="form-label small">From Month</label>
            <input
              type="month"
              className="form-control form-control-sm"
              value={range.from}
              onChange={(e) => setRange({ ...range, from: e.target.value })}
            />
          </div>
          <div className="col-md-3">
            <label className="form-label small">To Month</label>
            <input
              type="month"
              className="form-control form-control-sm"
              value={range.to}
              onChange={(e) => setRange({ ...range, to: e.target.value })}
            />
          </div>
          <div className="col-md-2">
            <button type="submit" className="btn btn-sm btn-primary w-100" disabled={loading}>
              {loading ? '...' : 'Filter'}
            </button>
          </div>
        </form>
      </div>

      {loading && <div className="text-center py-5">Loading analysis data...</div>}

      {error && <div className="alert alert-danger">{error}</div>}

      {data && (
        <div className="row g-4">
          {data.months.map((month, i) => (
            <div key={i} className="col-md-6 col-lg-4">
              <div className="stat-card">
                <div className="d-flex justify-content-between align-items-start mb-3">
                  <div>
                    <h3 className="h6 fw-bold mb-0">{month.label}</h3>
                    <span className="text-muted small">{month.transactionCount} transactions</span>
                  </div>
                  {month.warnings?.length > 0 && (
                    <span className="badge bg-warning text-dark" title={month.warnings.join('\n')}>!</span>
                  )}
                </div>

                <div className="row g-2 mb-3">
                  <div className="col-6">
                    <span className="text-muted small d-block lh-1 mb-1">Income</span>
                    <span className="text-success fw-bold small">{formatMoney(month.totalIncome, data.currency)}</span>
                  </div>
                  <div className="col-6">
                    <span className="text-muted small d-block lh-1 mb-1">Expenses</span>
                    <span className="text-danger fw-bold small">{formatMoney(month.totalExpense, data.currency)}</span>
                  </div>
                </div>

                <div className="border-top pt-2">
                  <div className="d-flex justify-content-between mb-1">
                    <span className="text-muted small">Savings</span>
                    <span className={`fw-bold small ${month.savings >= 0 ? 'text-primary' : 'text-danger'}`}>
                      {formatMoney(month.savings, data.currency)}
                    </span>
                  </div>
                  <div className="d-flex justify-content-between">
                    <span className="text-muted small">Closing Balance</span>
                    <span className="fw-semibold small">
                      {month.balanceAvailable ? formatMoney(month.closingBalance, data.currency) : 'N/A'}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}