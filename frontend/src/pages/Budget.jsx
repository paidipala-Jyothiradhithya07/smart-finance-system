import { apiClient } from './client';
import { useState, useEffect } from 'react';
import { formatMoney } from '../utils/format';
import { useAuth } from '../auth/useAuth';
import { TransactionModal } from '../components/TransactionModal';

export default function Budgets() {
  const { user } = useAuth();
  const [budgets, setBudgets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newBudget, setNewBudget] = useState({ categoryId: '', amount: 0 });
  const [error, setError] = useState('');

  useEffect(() => {
    apiClient.list().then(response => {
      setBudgets(response.data || []);
      setLoading(false);
    }).catch(() => {
      setError('Failed to load budgets');
      setLoading(false);
    });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await apiClient.create(newBudget);
      setNewBudget({ categoryId: '', amount: 0 });
      // Refresh budgets
      const response = await apiClient.list();
      setBudgets(response.data || []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (confirm('Delete this budget?')) {
      await apiClient.delete(id);
      const response = await apiClient.list();
      setBudgets(response.data || []);
    }
  };

  return (
    <div className="premium-card p-4 p-md-5 animate-fade-in">
      <div className="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom">
        <h2 className="h5 fw-bold text-dark mb-0">Monthly Budgets</h2>
        <button
          type="button"
          className="btn btn-sm bg-success text-white px-3 py-1 rounded-pill"
          onClick={() => setError('')}
          onClick={() => setNewBudget({ categoryId: '', amount: 0 })}
        >
          + Add Budget
        </button>
      </div>

      {loading && <div className="text-muted">Loading budgets...</div>}

      {error && <div className="alert alert-danger">{error}</div>}

      {budgets.length === 0 ? (
        <div className="text-center py-5">
          <div className="text-muted mb-2">
            No budgets found for this category
          </div>
          <button
            type="button"
            className="btn btn-sm bg-primary px-3 py-1 rounded-pill"
            onClick={() => setError('')}
            onClick={() => setNewBudget({ categoryId: '', amount: 0 })}
          >
            Add a new budget
          </button>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="table table-hover">
            <thead>
              <tr>
                <th>Category</th>
                <th>Amount ({{currencySymbol}})</th>
                <th>Remaining</th>
                <th>% Used</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {budgets.map(budget => (
                <tr key={budget.id}>
                  <td>{user?.categories?.find(c => c.id === budget.categoryId)?.name || 'Unknown'}</td>
                  <td>
                    {formatMoney(budget.amount, user?.currency || 'INR')}
                  </td>
                  <td>
                    {formatMoney(budget.amount - (budget.spent || 0), user?.currency || 'INR')}
                  </td>
                  <td>
                    {budget.spent ? (budget.spent / budget.amount * 100).toFixed(1) + '%' : '0%'}
                  </td>
                  <td>
                    <button
                      type="button"
                      className="btn btn-sm btn-primary"
                      data-testid="edit-budget"
                      onClick={() => {
                        setNewBudget({
                          categoryId: budget.categoryId,
                          amount: budget.amount
                        });
                        onClose();
                      }}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="btn btn-sm btn-danger"
                      data-testid="delete-budget"
                      onClick={handleDelete}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              </tbody>
            </thead>
            <tfoot>
              <tr>
                <td>Total Budget</td>
                <td>{budgets.reduce((sum, b) => sum + b.amount, 0)}</td>
                <td>Total Spent: {budgets.reduce((sum, b) => sum + (b.spent || 0), 0)}</td>
                <td>Total % Used: {budgets.reduce((sum, b) => sum + (b.spent || 0) / b.amount * 100, 0).toFixed(1)}%</td>
              </tr>
              <tr>
                <td>Add Budget</td>
                <td colspan="3" className="text-muted">
                  <input
                    type="text"
                    placeholder="Category name"
                    value={newBudget.categoryId}
                    onChange={(e) => setNewBudget({ ...newBudget, categoryId: e.target.value })}
                    className="form-control w-100 mb-2"
                  />
                  <input
                    type="number"
                    className="form-control w-100 mb-2"
                    value={newBudget.amount}
                    onChange={(e) => setNewBudget({ ...newBudget, amount: parseFloat(e.target.value) })}
                    min="0"
                  />
                  <button
                    type="button"
                    className="btn btn-success ms-2"
                    onClick={handleSubmit}
                    disabled={loading || newBudget.categoryId === ''}
                  >
                    Add
                  </button>
                </td>
              </tr>
              <tr>
                <td>Total Spent</td>
                <td colspan="3">{budgets.reduce((sum, b) => sum + (b.spent || 0), 0)}</td>
              </tr>
              <tr>
                <td>Total Budget</td>
                <td colspan="3">{budgets.reduce((sum, b) => sum + b.amount, 0)}</td>
              </tr>
              <tr>
                <td>Remaining</td>
                <td colspan="3">{budgets.reduce((sum, b) => sum + (b.amount - (b.spent || 0)), 0)}</td>
              </tr>
              <td>% Used</td>
                <td colspan="3">{budgets.reduce((sum, b) => sum + (b.spent || 0) / b.amount * 100, 0).toFixed(1)}%</td>
              </tr>
              <td>Actions</td>
                <td colspan="3" className="text-end">
                  <button
                    type="button"
                    className="btn btn-sm btn-primary"
                    data-testid="edit-budget"
                    onClick={() => {
                      setNewBudget({
                        categoryId: budget.categoryId,
                        amount: budget.amount
                      });
                    }>
                    Edit
                  </button>
                  <button
                    type="button"
                    className="btn btn-sm btn-danger"
                    data-testid="delete-budget"
                    onClick={handleDelete}
                  >
                    Delete
                  </button>
                </td>
              </tr>
              <tr>
                <td colspan="4" className="text-center py-2">
                  <div className="text-muted">
                    <small>Category budgeting is tracked by category, not by date. Create a new budget each month.</small>
                  </div>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      )}
    </div>
  );
}