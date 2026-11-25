import { useState, useEffect } from 'react';
import { useAuth } from '../auth/useAuth';
import { categoryApi } from '../api/categoryApi';
import { transactionApi } from '../api/transactionApi';
import { getErrorMessage, getFieldErrors } from '../utils/errors';
import { formatMoney } from '../utils/format';

export default function TransactionModal({ isOpen, onClose, transaction = null, onSubmit }) {
  const { user } = useAuth();
  const [transactionData, setTransactionData] = useState({
    categoryId: '',
    description: '',
    amount: 0,
    type: 'CREDIT',
    paymentMethod: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState([]);

  // Load categories on mount
  useEffect(() => {
    categoryApi.list()
      .then(response => setCategories(response.data || response))
      .catch(() => setCategories([]));
  }, []);

  // Load transaction data if editing
  useEffect(() => {
    if (transaction) {
      setTransactionData({
        ...transaction,
        categoryId: transaction.categoryId?.toString() || '',
        description: transaction.description || '',
        amount: transaction.amount || 0,
        type: transaction.type || 'CREDIT',
        paymentMethod: transaction.paymentMethod || '',
      });
    }
  }, [transaction]);

  const handleInputChange = (e) => {
    setTransactionData({ ...transactionData, [e.target.name]: e.target.value });
  };

  const handleAmountChange = (e) => {
    const value = parseFloat(e.target.value);
    setTransactionData({ ...transactionData, amount: isNaN(value) ? 0 : value });
  };

  const handleTypeChange = (e) => {
    setTransactionData({ ...transactionData, type: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const payload = {
        transactionDate: new Date().toISOString().split('T')[0],
        description: transactionData.description,
        amount: parseFloat(transactionData.amount),
        type: transactionData.type,
        categoryId: parseInt(transactionData.categoryId, 10),
        paymentMethod: transactionData.paymentMethod,
      };

      if (transaction) {
        await transactionApi.update(transaction.id, payload);
      } else {
        await transactionApi.create(payload);
      }

      onClose();
      if (onSubmit) onSubmit();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setTransactionData({
      categoryId: '',
      description: '',
      amount: 0,
      type: 'CREDIT',
      paymentMethod: '',
    });
    setError('');
    onClose();
  };

  const categoryOptions = categories.map(cat => ({
    id: cat.id,
    name: cat.name,
    type: cat.type,
  }));

  const filteredCategories = type === 'INCOME' || type === 'CREDIT'
    ? categoryOptions.filter(cat => cat.type === 'INCOME' || cat.type === 'BOTH')
    : categoryOptions.filter(cat => cat.type === 'EXPENSE' || cat.type === 'BOTH');

  const isIncome = transactionData.type === 'INCOME' || transactionData.type === 'CREDIT';

  return (
    <div className="modal-backdrop d-flex bg-black bg-opacity-50" onClick={handleClose}>
      <div className="modal-dialog modal-lg modal-dialog-centered" role="dialog" onClick={(e) => e.stopPropagation()}>
        <div className="modal-content bg-white rounded-lg shadow-lg">
          <div className="modal-header">
            <h5 className="modal-title">
              {transaction ? 'Edit Transaction' : 'Add Transaction'}
            </h5>
            <button
              type="button"
              className="btn-close"
              onClick={handleClose}
              aria-label="Close"
            />
          </div>
          <div className="modal-body">
            {error && <div className="alert alert-danger">{error}</div>}
            <form onSubmit={handleSubmit}>
              <div className="mb-3">
                <label className="form-label">Category</label>
                <select
                  name="categoryId"
                  className="form-control"
                  value={transactionData.categoryId}
                  onChange={handleInputChange}
                  disabled={loading}
                >
                  {filteredCategories.map(cat => (
                    <option key={cat.id} value={cat.id}>
                      {cat.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="mb-3">
                <label className="form-label">Description</label>
                <input
                  type="text"
                  name="description"
                  className="form-control"
                  value={transactionData.description}
                  onChange={handleInputChange}
                  disabled={loading}
                />
              </div>

              <div className="mb-3">
                <label className="form-label">Amount</label>
                <input
                  type="number"
                  name="amount"
                  className="form-control"
                  value={transactionData.amount}
                  onChange={handleAmountChange}
                  step="0.01"
                  min="0"
                  disabled={loading}
                />
              </div>

              <div className="mb-3">
                <label className="form-label">Type</label>
                <select
                  name="type"
                  className="form-control"
                  value={transactionData.type}
                  onChange={handleTypeChange}
                  disabled={loading}
                >
                  <option value="CREDIT">Income (Credit)</option>
                  <option value="DEBIT">Expense (Debit)</option>
                </select>
              </div>

              <div className="mb-3">
                <label className="form-label">Payment Method</label>
                <input
                  type="text"
                  className="form-control"
                  name="paymentMethod"
                  value={transactionData.paymentMethod}
                  onChange={handleInputChange}
                  disabled={loading}
                />
              </div>

              {loading && <div className="alert alert-info">Saving...</div>}
              {error && <div className="alert alert-danger">{error}</div>}

              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Saving...' : 'Save Transaction'}
              </button>
              <button
                type="button"
                className="btn btn-secondary ms-2"
                onClick={handleClose}
              >
                Cancel
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}