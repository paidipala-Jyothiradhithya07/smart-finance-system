import { apiClient } from './client';
import { useState, useEffect } from 'react';
import { formatMoney } from '../utils/format';
import { useAuth } from '../auth/useAuth';
import { TransactionModal } from '../components/TransactionModal';
import { formatDate } from '../utils/format';

export default function Transactions() {
  const { user } = useAuth();
  const [transactions, setTransactions] = useState([]);
  const [filters, setFilters] = useState({
    from: '',
    to: '',
    categoryId: '',
    type: 'all',
    minAmount: 0,
    maxAmount: 100000,
    q: '',
    source: 'all'
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Load transactions on mount
  useEffect(() => {
    loadTransactions();
  }, []);

  const loadTransactions = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.list({
        page: total,
        size: pageSize,
        ...filters,
        sortBy: 'transactionDate',
        direction: 'desc'
      });
      setTransactions(response.data || []);
      setTotal(response.totalElements || 0);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters({ ...filters, [name]: value });
    setTotal(0);
    setCurrentPage(1);
    loadTransactions();
  };

  const handleDelete = async (id) => {
    if (confirm('Delete this transaction?')) {
      await apiClient.remove(id);
      setError('');
      setLoading(true);
      loadTransactions();
    }
  };

  const handleEdit = (transaction) => {
    // This would open the TransactionModal with transaction data
    // For now, we'll just show a placeholder
    alert(`Edit transaction: ${transaction.description} (${transaction.amount})`);
  };

  const formatCurrency = (amount) => formatMoney(amount, user?.currency || 'INR');

  const handlePageChange = (page) => {
    setCurrentPage(page);
    setLoading(true);
    loadTransactions();
  };

  const handleSizeChange = (size) => {
    setPageSize(size);
    setCurrentPage(1);
    setLoading(true);
    loadTransactions();
  };

  const formatDateRange = () => {
    const from = filters.from ? new Date(filters.from) : null;
    const to = filters.to ? new Date(filters.to) : null;
    return `${from ? formatDate(from) : 'Any'} to ${to ? formatDate(to) : 'Any'}`;
  };

  return (
    <div className="container my-4">
      <h1 className="mb-4">Transactions</h1>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="row mb-4">
        <div className="col-md-3">
          <div className="mb-3">
            <label className="form-label">Date Range</label>
            <div className="input-group">
              <input
                type="date"
                name="from"
                className="form-control"
                value={filters.from}
                onChange={handleFilterChange}
                value={filters.from}
              />
              <input
                type="date"
                name="to"
                className="form-control"
                value={filters.to}
                onChange={handleFilterChange}
                value={filters.to}
              />
            </div>
          </div>
        </div>

        <div className="col-md-3">
          <div className="mb-3">
            <label className="form-label">Category</label>
            <select
              name="categoryId"
              className="form-control"
              value={filters.categoryId}
              onChange={handleFilterChange}
            >
              <option value="">All Categories</option>
              {user?.categories?.map(cat => (
                <option key={cat.id} value={cat.id}>
                  {cat.name} ({cat.type === 'INCOME' ? 'Income' : 'Expense'})
                </option>
              </option>
            </select>
          </div>
        </div>

        <div className="col-md-3">
          <div className="mb-3">
            <label className="form-label">Type</label>
            <select
              name="type"
              className="form-control"
              value={filters.type}
              onChange={handleFilterChange}
            >
              <option value="all">All</option>
              <option value="CREDIT">Income</option>
              <option value="DEBIT">Expense</option>
            </select>
          </div>
        </div>

        <div className="col-md-3">
          <div className="mb-3">
            <label className="form-label">Amount Range</label>
            <div className="input-group">
              <input
                type="number"
                name="minAmount"
                className="form-control"
                value={filters.minAmount}
                onChange={handleFilterChange}
                min="0"
                step="1"
                placeholder="Min amount"
              />
              <input
                type="number"
                name="maxAmount"
                className="form-control"
                value={filters.maxAmount}
                onChange={handleFilterChange}
                min="0"
                step="1"
                placeholder="Max amount"
              />
            </div>
          </div>
        </div>

        <div className="col-md-3">
          <div className="mb-3">
            <label className="form-label">Search</label>
            <input
              type="text"
              name="q"
              className="form-control"
              value={filters.q}
              onChange={handleFilterChange}
              placeholder="Search by description or reference number"
            />
          </div>
        </div>

        <div className="col-md-3">
          <div className="mb-3">
            <label className="form-label">Source</label>
            <select
              name="source"
              className="form-control"
              value={filters.source}
              onChange={handleFilterChange}
            >
              <option value="all">All Sources</option>
              <option value="MANUAL">Manual Entry</option>
              <option value="BANK_STATEMENT">Bank Statement</option>
            </select>
          </div>
        </div>
      </div>

      {loading && <div className="text-center py-4">Loading transactions...</div>}

      {error && <div className="alert alert-danger">{error}</div>}

      {transactions.length > 0 ? (
        <div className="table-responsive">
          <table className="table table-hover">
            <thead>
              <tr>
                <th>Date</th>
                <th>Description</th>
                <th>Category</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Balance</th>
                <th>Source</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map(transaction => (
                <tr key={transaction.id} data-testid={`transaction-row-${transaction.id}`}>
                  <td>{formatDate(transaction.transactionDate)}</td>
                  <td>{transaction.description}</td>
                  <td>
                    {user?.categories?.find(c => c.id === transaction.categoryId)?.name || 'Unknown'}
                  </td>
                  <td>
                    <span className={`badge ${transaction.type === 'CREDIT' ? 'bg-success' : 'bg-danger'} text-white}`}>
                      {transaction.type === 'CREDIT' ? 'Income' : 'Expense'}
                    </span>
                  </td>
                  <td>{formatCurrency(transaction.amount)}</td>
                  <td>{transaction.balanceAfter !== null ? formatCurrency(transaction.balanceAfter) : 'N/A'}</td>
                  <td>{transaction.source}</td>
                  <td>
                    <button
                      type="button"
                      className="btn btn-sm btn-primary"
                      data-testid={`edit-transaction-${transaction.id}`}
                      onClick={() => handleEdit(transaction)}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="btn btn-sm btn-danger"
                      data-testid={`delete-transaction-${transaction.id}`}
                      onClick={() => handleDelete(transaction.id)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              </tbody>
            </thead>
            <tfoot>
              <tr>
                <td>Total: {total} transactions</td>
                <td colspan="7">
                  Page {total ? Math.ceil(total / pageSize) : 0} of {Math.ceil(total / pageSize)}
                  | Showing {Math.min(total, (currentPage - 1) * pageSize + 1)} to {Math.min(total, currentPage * pageSize)}
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      ) : (
        <div className="text-center py-5">
          <div className="text-muted">
            No transactions found.
            {filters.q && <p className="text-muted">Try clearing the search filter.</p>}
          </div>
        </div>
      )}

      <div className="text-center mt-4">
        <div className="d-flex justify-content-between">
          <div>
            <button
              type="button"
              className="btn btn-sm btn-outline-primary"
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 1 || !loading}
            >
              Previous
            </button>
            <span className="text-muted">Page {currentPage} of {Math.ceil(total / pageSize)}</div>
            <button
              type="button"
              className="btn btn-sm btn-outline-primary"
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage * pageSize >= total || !loading}
            >
              Next
            </button>
          </div>
          <div>
            <select className="form-select"
                    value={pageSize}
                    onChange={(e) => handleSizeChange(parseInt(e.target.value))}
            >
              <option value="5">5</option>
              <option value="10" selected>10</option>
              <option value="20">10</option>
              <option value="50">50</option>
            </select>
          </div>
        </div>
      }
    </div>
  );
}