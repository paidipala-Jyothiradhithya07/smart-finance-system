import { useState, useEffect } from 'react';
import { apiClient } from '../api/client';
import { formatMoney } from '../utils/format';
import { useAuth } from '../auth/useAuth';
import { getErrorMessage } from '../utils/errors';

export default function Budgets() {
  const { user } = useAuth();
  const [budgets, setBudgets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newBudget, setNewBudget] = useState({ categoryId: '', amount: 0 });
  const [error, setError] = useState('');

  const currencySymbol = user?.currency === 'INR' ? '₹' : user?.currency === 'GBP' ? '£' : '$';

  useEffect(() => {
    let ignore = false;

    apiClient.get('/api/budgets')
      .then((response) => {
        if (!ignore) {
          setBudgets(response.data || []);
          setLoading(false);
        }
      })
      .catch(() => {
        if (!ignore) {
          setBudgets([]);
          setLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, []);

  const loadBudgets = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get('/api/budgets');
      setBudgets(response.data || []);
    } catch {
      setBudgets([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await apiClient.post('/api/budgets', newBudget);
      setNewBudget({ categoryId: '', amount: 0 });
      await loadBudgets();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Delete this budget?')) {
      try {
        await apiClient.delete(`/api/budgets/${id}`);
        await loadBudgets();
      } catch (err) {
        setError(getErrorMessage(err));
      }
    }
  };

  const totalBudget = budgets.reduce((sum, b) => sum + (Number(b.amount) || 0), 0);
  const totalSpent = budgets.reduce((sum, b) => sum + (Number(b.spent) || 0), 0);
  const totalPercentUsed = totalBudget > 0 ? ((totalSpent / totalBudget) * 100).toFixed(1) : '0.0';

  return (
    <div className="premium-card p-4 p-md-5 animate-fade-in">
      <div className="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom">
        <h2 className="h5 fw-bold text-dark mb-0">Monthly Budgets</h2>
        <button
          type="button"
          className="btn btn-sm btn-success px-3 py-1 rounded-pill"
          onClick={() => setNewBudget({ categoryId: '', amount: 0 })}
        >
          + Add Budget
        </button>
      </div>

      {loading && <div className="text-muted mb-3">Loading budgets...</div>}

      {error && <div className="alert alert-danger mb-3">{error}</div>}

      {budgets.length === 0 ? (
        <div className="text-center py-5">
          <div className="text-muted mb-3">
            No budgets found for this category
          </div>
          <div className="card p-3 mx-auto" style={{ maxWidth: '400px' }}>
            <h6 className="fw-semibold mb-2">Create New Budget</h6>
            <form onSubmit={handleSubmit}>
              <input
                type="text"
                placeholder="Category ID or Name"
                value={newBudget.categoryId}
                onChange={(e) => setNewBudget({ ...newBudget, categoryId: e.target.value })}
                className="form-control mb-2"
                required
              />
              <input
                type="number"
                placeholder="Amount"
                className="form-control mb-2"
                value={newBudget.amount || ''}
                onChange={(e) => setNewBudget({ ...newBudget, amount: parseFloat(e.target.value) || 0 })}
                min="0"
                step="0.01"
                required
              />
              <button
                type="submit"
                className="btn btn-primary btn-sm w-100"
                disabled={loading || !newBudget.categoryId}
              >
                Save Budget
              </button>
            </form>
          </div>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="table table-hover">
            <thead>
              <tr>
                <th>Category</th>
                <th>Amount ({currencySymbol})</th>
                <th>Remaining</th>
                <th>% Used</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {budgets.map((budget) => (
                <tr key={budget.id}>
                  <td>{user?.categories?.find((c) => c.id === budget.categoryId)?.name || budget.categoryName || 'Unknown'}</td>
                  <td>{formatMoney(budget.amount, user?.currency || 'INR')}</td>
                  <td>{formatMoney((budget.amount || 0) - (budget.spent || 0), user?.currency || 'INR')}</td>
                  <td>{budget.amount ? `${((budget.spent || 0) / budget.amount * 100).toFixed(1)}%` : '0%'}</td>
                  <td>
                    <button
                      type="button"
                      className="btn btn-sm btn-primary me-2"
                      data-testid="edit-budget"
                      onClick={() => setNewBudget({ categoryId: budget.categoryId, amount: budget.amount })}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="btn btn-sm btn-danger"
                      data-testid="delete-budget"
                      onClick={() => handleDelete(budget.id)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td>Total Budget</td>
                <td>{totalBudget}</td>
                <td>Total Spent: {totalSpent}</td>
                <td>Total % Used: {totalPercentUsed}%</td>
                <td></td>
              </tr>
              <tr>
                <td>Add Budget</td>
                <td colSpan={4}>
                  <form onSubmit={handleSubmit} className="d-flex gap-2">
                    <input
                      type="text"
                      placeholder="Category"
                      value={newBudget.categoryId}
                      onChange={(e) => setNewBudget({ ...newBudget, categoryId: e.target.value })}
                      className="form-control form-control-sm"
                    />
                    <input
                      type="number"
                      placeholder="Amount"
                      className="form-control form-control-sm"
                      value={newBudget.amount || ''}
                      onChange={(e) => setNewBudget({ ...newBudget, amount: parseFloat(e.target.value) || 0 })}
                      min="0"
                      step="0.01"
                    />
                    <button
                      type="submit"
                      className="btn btn-success btn-sm text-nowrap"
                      disabled={loading || !newBudget.categoryId}
                    >
                      Add
                    </button>
                  </form>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      )}
    </div>
  );
}
