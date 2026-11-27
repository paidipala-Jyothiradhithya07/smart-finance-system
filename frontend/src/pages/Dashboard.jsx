import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { getOverview, getMonthlySummary } from '../api/finance';
import { getTransactions } from '../api/transactions';
import { formatMoney, formatDate } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import Spinner from '../components/Spinner';
import ErrorAlert from '../components/ErrorAlert';
import EmptyState from '../components/EmptyState';

export default function Dashboard() {
  const { user } = useAuth();
  const [overview, setOverview] = useState(null);
  const [monthlySummary, setMonthlySummary] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;

    Promise.all([
      getOverview(),
      getMonthlySummary(),
      getTransactions({ size: 5 }),
    ])
      .then(([overviewData, monthlyData, txData]) => {
        if (!ignore) {
          setOverview(overviewData);
          setMonthlySummary(monthlyData);
          setTransactions(Array.isArray(txData) ? txData : (txData?.content || []));
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
  }, []);

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center py-5">
        <Spinner text="Loading dashboard..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="py-4">
        <ErrorAlert message={error} />
      </div>
    );
  }

  const months = monthlySummary?.months || [];
  const latestMonth = months.length > 0 ? months[months.length - 1] : null;
  const monthLabel = latestMonth?.label;

  const renderSavings = () => {
    if (!latestMonth || latestMonth.savings === null || latestMonth.savings === undefined) {
      return (
        <span title="Not available" className="text-muted">
          Not available
        </span>
      );
    }

    const val = latestMonth.savings;
    const formatted = formatMoney(val, user?.currency);

    if (val > 0) {
      return <span className="text-success fw-bold">+{formatted}</span>;
    }
    if (val < 0) {
      return <span className="text-danger fw-bold">{formatted}</span>;
    }
    return <span className="fw-bold">{formatted}</span>;
  };

  return (
    <div className="py-2">
      <div className="row g-3 mb-4">
        {/* Total balance card */}
        <div className="col-12 col-sm-6 col-lg-3">
          <div className="card h-100 shadow-sm border-0" data-testid="dashboard-balance">
            <div className="card-body">
              <h6 className="card-subtitle text-muted mb-2">Total balance</h6>
              <div className="fs-4 fw-bold">
                {overview?.closingBalance !== null && overview?.closingBalance !== undefined ? (
                  formatMoney(overview.closingBalance, user?.currency)
                ) : (
                  <span title="Balance not available" className="text-muted">
                    Not available
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Income card */}
        <div className="col-12 col-sm-6 col-lg-3">
          <div className="card h-100 shadow-sm border-0" data-testid="dashboard-income">
            <div className="card-body">
              <h6 className="card-subtitle text-muted mb-2">
                {monthLabel ? `Income (${monthLabel})` : 'Income'}
              </h6>
              <div className="fs-4 fw-bold">
                {latestMonth && latestMonth.totalIncome !== null && latestMonth.totalIncome !== undefined ? (
                  formatMoney(latestMonth.totalIncome, user?.currency)
                ) : (
                  <span title="Not available" className="text-muted">
                    Not available
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Expenses card */}
        <div className="col-12 col-sm-6 col-lg-3">
          <div className="card h-100 shadow-sm border-0" data-testid="dashboard-expenses">
            <div className="card-body">
              <h6 className="card-subtitle text-muted mb-2">
                {monthLabel ? `Expenses (${monthLabel})` : 'Expenses'}
              </h6>
              <div className="fs-4 fw-bold">
                {latestMonth && latestMonth.totalExpense !== null && latestMonth.totalExpense !== undefined ? (
                  formatMoney(latestMonth.totalExpense, user?.currency)
                ) : (
                  <span title="Not available" className="text-muted">
                    Not available
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Savings card */}
        <div className="col-12 col-sm-6 col-lg-3">
          <div className="card h-100 shadow-sm border-0" data-testid="dashboard-savings">
            <div className="card-body">
              <h6 className="card-subtitle text-muted mb-2">
                {monthLabel ? `Savings (${monthLabel})` : 'Savings'}
              </h6>
              <div className="fs-4">{renderSavings()}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Recent transactions section */}
      <div className="mt-4">
        <h2 className="h5 mb-3 fw-bold">Recent transactions</h2>
        {transactions.length === 0 ? (
          <EmptyState
            title="No recent transactions"
            description={
              <span>
                <span className="d-block mb-3">
                  No transactions found. Upload a statement or add a transaction to get started.
                </span>
                <span className="d-inline-flex gap-2">
                  <Link to="/upload" role="button" className="btn btn-primary">
                    Upload statement
                  </Link>
                  <Link to="/transactions/new" role="button" className="btn btn-outline-primary">
                    Add transaction
                  </Link>
                </span>
              </span>
            }
          />
        ) : (
          <div className="card shadow-sm border-0">
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0" data-testid="transactions-table">
                <thead className="table-light">
                  <tr>
                    <th scope="col">Date</th>
                    <th scope="col">Description</th>
                    <th scope="col">Category</th>
                    <th scope="col">Type</th>
                    <th scope="col">Amount</th>
                    <th scope="col">Source</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((tx) => (
                    <tr key={tx.id} data-testid={`transaction-row-${tx.id}`}>
                      <td>{formatDate(tx.transactionDate)}</td>
                      <td>{tx.description}</td>
                      <td>{tx.categoryName || '—'}</td>
                      <td>
                        {tx.type === 'CREDIT' ? (
                          <span className="badge bg-success">+ Credit</span>
                        ) : (
                          <span className="badge bg-danger">- Debit</span>
                        )}
                      </td>
                      <td>{formatMoney(tx.amount, user?.currency)}</td>
                      <td>
                        <span className="badge bg-light text-dark border">
                          {tx.source === 'BANK_STATEMENT' ? 'Bank' : 'Manual'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
