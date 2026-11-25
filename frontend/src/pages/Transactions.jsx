import { useState, useEffect } from 'react';
import { formatMoney, formatDate } from '../utils/format';
import { useAuth } from '../auth/useAuth';
import { transactionApi } from '../api/transactionApi';
import { getErrorMessage } from '../utils/errors';
import TransactionModal from '../components/TransactionModal';

export default function Transactions() {
  const { user } = useAuth();
  const [transactions, setTransactions] = useState([]);
  const [filters, setFilters] = useState({
    from: '',
    to: '',
    categoryId: '',
    type: '',
    minAmount: '',
    maxAmount: '',
    q: '',
    source: '',
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState(null);

  useEffect(() => {
    let ignore = false;

    const cleanFilters = {};
    Object.keys(filters).forEach((key) => {
      if (filters[key] !== '' && filters[key] !== 'all') {
        cleanFilters[key] = filters[key];
      }
    });

    transactionApi.list({
      page: currentPage - 1,
      size: pageSize,
      ...cleanFilters,
      sortBy: 'transactionDate',
      direction: 'desc',
    })
      .then((response) => {
        if (!ignore) {
          const data = response.data || {};
          setTransactions(data.content || []);
          setTotal(data.totalElements || 0);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (!ignore) {
          setError(getErrorMessage(err));
          setLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, [currentPage, pageSize, filters]);

  const loadTransactions = async () => {
    setLoading(true);
    setError('');
    try {
      const cleanFilters = {};
      Object.keys(filters).forEach((key) => {
        if (filters[key] !== '' && filters[key] !== 'all') {
          cleanFilters[key] = filters[key];
        }
      });

      const response = await transactionApi.list({
        page: currentPage - 1,
        size: pageSize,
        ...cleanFilters,
        sortBy: 'transactionDate',
        direction: 'desc',
      });
      const data = response.data || {};
      setTransactions(data.content || []);
      setTotal(data.totalElements || 0);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
    setCurrentPage(1);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Delete this transaction?')) {
      try {
        await transactionApi.remove(id);
        await loadTransactions();
      } catch (err) {
        setError(getErrorMessage(err));
      }
    }
  };

  const handleEdit = (transaction) => {
    setEditingTransaction(transaction);
    setIsModalOpen(true);
  };

  const handleAddNew = () => {
    setEditingTransaction(null);
    setIsModalOpen(true);
  };

  const formatCurrency = (amount) => formatMoney(amount, user?.currency || 'INR');

  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  return (
    <div className="container my-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1>Transactions</h1>
        <button
          type="button"
          className="btn btn-primary"
          onClick={handleAddNew}
        >
          Add Transaction
        </button>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="row g-3 mb-4">
        <div className="col-md-3">
          <div className="mb-3">
            <label className="form-label">From Date</label>
            <input
              type="date"
              name="from"
              className="form-control"
              onChange={handleFilterChange}
              value={filters.from}
            />
          </div>
        </div>

        <div className="col-md-3">
          <div className="mb-3">
            <label className="form-label">To Date</label>
            <input
              type="date"
              name="to"
              className="form-control"
              onChange={handleFilterChange}
              value={filters.to}
            />
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
              {user?.categories?.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name} ({cat.type === 'INCOME' ? 'Income' : 'Expense'})
                </option>
              ))}
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
              <option value="">All Types</option>
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
                placeholder="Min"
              />
              <input
                type="number"
                name="maxAmount"
                className="form-control"
                value={filters.maxAmount}
                onChange={handleFilterChange}
                min="0"
                step="1"
                placeholder="Max"
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
              placeholder="Description or reference"
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
              <option value="">All Sources</option>
              <option value="MANUAL">Manual Entry</option>
              <option value="BANK_STATEMENT">Bank Statement</option>
            </select>
          </div>
        </div>
      </div>

      {loading && <div className="text-center py-4">Loading transactions...</div>}

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
              {transactions.map((transaction) => (
                <tr key={transaction.id} data-testid={`transaction-row-${transaction.id}`}>
                  <td>{formatDate(transaction.transactionDate)}</td>
                  <td>{transaction.description}</td>
                  <td>
                    {user?.categories?.find((c) => c.id === transaction.categoryId)?.name || transaction.categoryName || 'Unknown'}
                  </td>
                  <td>
                    <span className={`badge ${transaction.type === 'CREDIT' ? 'bg-success' : 'bg-danger'} text-white`}>
                      {transaction.type === 'CREDIT' ? 'Income' : 'Expense'}
                    </span>
                  </td>
                  <td>{formatCurrency(transaction.amount)}</td>
                  <td>{transaction.balanceAfter !== null && transaction.balanceAfter !== undefined ? formatCurrency(transaction.balanceAfter) : 'N/A'}</td>
                  <td>{transaction.source}</td>
                  <td>
                    <button
                      type="button"
                      className="btn btn-sm btn-primary me-2"
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
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td>Total: {total} transactions</td>
                <td colSpan="7">
                  Page {currentPage} of {totalPages}
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      ) : (
        !loading && (
          <div className="text-center py-5">
            <div className="text-muted">
              No transactions found.
              {filters.q && <p className="text-muted">Try clearing the search filter.</p>}
            </div>
          </div>
        )
      )}

      <div className="d-flex justify-content-between align-items-center mt-4">
        <div>
          <button
            type="button"
            className="btn btn-sm btn-outline-primary me-2"
            onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
            disabled={currentPage === 1 || loading}
          >
            Previous
          </button>
          <span className="text-muted">
            Page {currentPage} of {totalPages}
          </span>
          <button
            type="button"
            className="btn btn-sm btn-outline-primary ms-2"
            onClick={() => setCurrentPage((p) => p + 1)}
            disabled={currentPage >= totalPages || loading}
          >
            Next
          </button>
        </div>
        <div>
          <select
            className="form-select form-select-sm"
            value={pageSize}
            onChange={(e) => {
              setPageSize(parseInt(e.target.value, 10));
              setCurrentPage(1);
            }}
          >
            <option value="5">5</option>
            <option value="10">10</option>
            <option value="20">20</option>
            <option value="50">50</option>
          </select>
        </div>
      </div>

      <TransactionModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        transaction={editingTransaction}
        onSubmit={loadTransactions}
      />
    </div>
  );
}
