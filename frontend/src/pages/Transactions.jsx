import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { getTransactions, deleteTransaction } from '../api/transactions';
import { getCategories } from '../api/categories';
import { getErrorMessage, getFieldErrors } from '../utils/errors';
import { formatDate, formatMoney } from '../utils/format';
import { useDebounce } from '../utils/useDebounce';
import Spinner from '../components/Spinner';
import ErrorAlert from '../components/ErrorAlert';
import EmptyState from '../components/EmptyState';
import ConfirmDialog from '../components/ConfirmDialog';

const PAGE_SIZES = [10, 20, 50];
const DEFAULT_SIZE = 20;
const SORT_FIELDS = [
  { value: 'transactionDate', label: 'Transaction date' },
  { value: 'amount', label: 'Amount' },
  { value: 'createdAt', label: 'Date added' },
];
const SOURCES = [
  { value: 'MANUAL', label: 'Manual' },
  { value: 'BANK_STATEMENT', label: 'Bank statement' },
];
const TITLES = { CREDIT: 'Income', DEBIT: 'Expenses' };
const AMOUNT_PATTERN = /^\d{1,13}(\.\d{1,2})?$/;

// fixedType: "CREDIT" for the Income page, "DEBIT" for the Expenses page, null for all transactions.
export default function Transactions({ fixedType = null }) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const currency = user?.currency ?? 'INR';
  const [searchParams, setSearchParams] = useSearchParams();

  // ---- every filter is read from the URL (same names as the API) ----
  const from = searchParams.get('from') ?? '';
  const to = searchParams.get('to') ?? '';
  const categoryId = searchParams.get('categoryId') ?? '';
  const type = fixedType ?? searchParams.get('type') ?? '';
  const minAmount = searchParams.get('minAmount') ?? '';
  const maxAmount = searchParams.get('maxAmount') ?? '';
  const source = searchParams.get('source') ?? '';
  const q = searchParams.get('q') ?? '';

  const pageNumber = Number.parseInt(searchParams.get('page') ?? '', 10);
  const page = Number.isInteger(pageNumber) && pageNumber > 0 ? pageNumber : 0;
  const sizeNumber = Number.parseInt(searchParams.get('size') ?? '', 10);
  const size = PAGE_SIZES.includes(sizeNumber) ? sizeNumber : DEFAULT_SIZE;
  const sortParam = searchParams.get('sortBy');
  const sortBy = SORT_FIELDS.some((field) => field.value === sortParam) ? sortParam : 'transactionDate';
  const direction = searchParams.get('direction') === 'asc' ? 'asc' : 'desc';

  const hasFilters = Boolean(
    from || to || categoryId || (type && !fixedType) || minAmount || maxAmount || source || q,
  );

  // ---- loading the list ----
  const [reloadCount, setReloadCount] = useState(0);
  const [result, setResult] = useState({ key: null, data: null, error: null });

  // Only the non-empty filters are sent. The JSON string is the "identity" of one request.
  const paramsKey = JSON.stringify(
    Object.fromEntries(
      Object.entries({
        from, to, categoryId, type, minAmount, maxAmount, source, q: q.trim(), page, size, sortBy, direction,
      }).filter(([, value]) => value !== '' && value !== null && value !== undefined),
    ),
  );
  const requestKey = `${paramsKey}|${reloadCount}`;

  useEffect(() => {
    let cancelled = false;
    getTransactions(JSON.parse(paramsKey))
      .then((data) => {
        if (!cancelled) {
          setResult({ key: requestKey, data, error: null });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setResult({ key: requestKey, data: null, error });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [paramsKey, reloadCount, requestKey]);

  // We are "loading" whenever the stored result belongs to an older request
  const loading = result.key !== requestKey;
  const data = loading ? null : result.data;
  const loadError = loading ? null : result.error;
  const content = data?.content ?? [];

  // ---- categories for the filter (loaded once; no type, so "Other" appears only once) ----
  const [categories, setCategories] = useState([]);
  const categoryType = fixedType === 'CREDIT' ? 'INCOME' : fixedType === 'DEBIT' ? 'EXPENSE' : undefined;

  useEffect(() => {
    let cancelled = false;
    getCategories(categoryType)
      .then((list) => {
        if (!cancelled) {
          setCategories(list);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCategories([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [categoryType]);

  // ---- changing the URL ----
  const updateParams = (changes, { keepPage = false } = {}) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(changes).forEach(([key, value]) => {
      if (value === '' || value === null || value === undefined) {
        next.delete(key);
      } else {
        next.set(key, String(value));
      }
    });
    if (!keepPage) {
      next.delete('page'); // any filter change goes back to the first page
    }
    setSearchParams(next, { replace: true });
  };

  const goToPage = (target) => updateParams({ page: target > 0 ? target : '' }, { keepPage: true });

  // ---- keyword: typed text is kept locally, then copied into the URL after 400 ms ----
  const [qInput, setQInput] = useState(q);
  const [seenQ, setSeenQ] = useState(q);
  if (q !== seenQ) {
    // The URL changed from outside (Back button, Clear filters): follow it
    setSeenQ(q);
    if (q !== qInput.trim()) {
      setQInput(q);
    }
  }
  const debouncedQ = useDebounce(qInput, 400);

  const setParamsRef = useRef(setSearchParams);
  useEffect(() => {
    setParamsRef.current = setSearchParams;
  });

  useEffect(() => {
    setParamsRef.current(
      (previous) => {
        const trimmed = debouncedQ.trim();
        if ((previous.get('q') ?? '') === trimmed) {
          return previous;
        }
        const next = new URLSearchParams(previous);
        if (trimmed) {
          next.set('q', trimmed);
        } else {
          next.delete('q');
        }
        next.delete('page');
        return next;
      },
      { replace: true },
    );
  }, [debouncedQ]);

  // ---- amount filters are applied when the box loses focus or Enter is pressed ----
  const [amountNotice, setAmountNotice] = useState('');
  const applyAmount = (key, rawValue) => {
    const value = rawValue.trim();
    if (value !== '' && !AMOUNT_PATTERN.test(value)) {
      setAmountNotice('Amounts must look like 1000 or 99.50.');
      return;
    }
    setAmountNotice('');
    if (value !== (key === 'minAmount' ? minAmount : maxAmount)) {
      updateParams({ [key]: value });
    }
  };

  const clearFilters = () => {
    setQInput('');
    setAmountNotice('');
    setSearchParams({}, { replace: true });
  };

  // ---- delete ----
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const confirmDelete = async () => {
    if (!toDelete) {
      return;
    }
    setDeleting(true);
    try {
      await deleteTransaction(toDelete.id);
      const wasLastRowOnPage = content.length === 1 && page > 0;
      setToDelete(null);
      if (wasLastRowOnPage) {
        goToPage(page - 1);
      } else {
        setReloadCount((count) => count + 1);
      }
    } catch (error) {
      setToDelete(null);
      setDeleteError(error);
    } finally {
      setDeleting(false);
    }
  };

  const handleAdd = () => navigate(fixedType ? `/transactions/new?type=${fixedType}` : '/transactions/new');
  const title = fixedType ? TITLES[fixedType] : 'Transactions';

  // ---- what to show instead of the table when there are no rows ----
  const pastLastPage = Boolean(data) && data.totalElements > 0 && content.length === 0;

  return (
    <div className="container py-4">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h1 className="h3 mb-0">{title}</h1>
        <button type="button" className="btn btn-primary" onClick={handleAdd} data-testid="add-transaction">
          Add transaction
        </button>
      </div>

      {/* ---------- filter bar ---------- */}
      <form className="card card-body mb-3" onSubmit={(event) => event.preventDefault()} aria-label="Filters">
        <div className="row g-2">
          <div className="col-6 col-md-3">
            <label htmlFor="f-from" className="form-label small mb-1">From</label>
            <input
              id="f-from"
              type="date"
              className="form-control"
              value={from}
              onChange={(event) => updateParams({ from: event.target.value })}
            />
          </div>
          <div className="col-6 col-md-3">
            <label htmlFor="f-to" className="form-label small mb-1">To</label>
            <input
              id="f-to"
              type="date"
              className="form-control"
              value={to}
              onChange={(event) => updateParams({ to: event.target.value })}
            />
          </div>
          <div className="col-6 col-md-3">
            <label htmlFor="f-category" className="form-label small mb-1">Category</label>
            <select
              id="f-category"
              className="form-select"
              value={categoryId}
              onChange={(event) => updateParams({ categoryId: event.target.value })}
            >
              <option value="">All categories</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
          </div>
          {!fixedType && (
            <div className="col-6 col-md-3">
              <label htmlFor="f-type" className="form-label small mb-1">Type</label>
              <select
                id="f-type"
                className="form-select"
                value={type}
                onChange={(event) => updateParams({ type: event.target.value })}
              >
                <option value="">Income and expenses</option>
                <option value="CREDIT">Income (credit)</option>
                <option value="DEBIT">Expense (debit)</option>
              </select>
            </div>
          )}
          <div className="col-6 col-md-3">
            <label htmlFor="f-min" className="form-label small mb-1">Min amount</label>
            <input
              id="f-min"
              key={`min-${minAmount}`}
              className="form-control"
              inputMode="decimal"
              defaultValue={minAmount}
              onBlur={(event) => applyAmount('minAmount', event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  applyAmount('minAmount', event.currentTarget.value);
                }
              }}
            />
          </div>
          <div className="col-6 col-md-3">
            <label htmlFor="f-max" className="form-label small mb-1">Max amount</label>
            <input
              id="f-max"
              key={`max-${maxAmount}`}
              className="form-control"
              inputMode="decimal"
              defaultValue={maxAmount}
              onBlur={(event) => applyAmount('maxAmount', event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  applyAmount('maxAmount', event.currentTarget.value);
                }
              }}
            />
          </div>
          <div className="col-6 col-md-3">
            <label htmlFor="f-source" className="form-label small mb-1">Source</label>
            <select
              id="f-source"
              className="form-select"
              value={source}
              onChange={(event) => updateParams({ source: event.target.value })}
            >
              <option value="">All sources</option>
              {SOURCES.map((item) => (
                <option key={item.value} value={item.value}>{item.label}</option>
              ))}
            </select>
          </div>
          <div className="col-6 col-md-3">
            <label htmlFor="f-q" className="form-label small mb-1">Search</label>
            <input
              id="f-q"
              type="search"
              className="form-control"
              placeholder="Description or reference"
              maxLength={100}
              value={qInput}
              onChange={(event) => setQInput(event.target.value)}
              data-testid="transactions-search"
            />
          </div>
        </div>

        {amountNotice && <div className="text-danger small mt-2" role="alert">{amountNotice}</div>}

        <div className="row g-2 mt-1 align-items-end">
          <div className="col-6 col-md-3">
            <label htmlFor="f-sort" className="form-label small mb-1">Sort by</label>
            <select
              id="f-sort"
              className="form-select"
              value={sortBy}
              onChange={(event) => updateParams({ sortBy: event.target.value === 'transactionDate' ? '' : event.target.value })}
            >
              {SORT_FIELDS.map((field) => (
                <option key={field.value} value={field.value}>{field.label}</option>
              ))}
            </select>
          </div>
          <div className="col-6 col-md-3">
            <label htmlFor="f-direction" className="form-label small mb-1">Order</label>
            <select
              id="f-direction"
              className="form-select"
              value={direction}
              onChange={(event) => updateParams({ direction: event.target.value === 'desc' ? '' : event.target.value })}
            >
              <option value="desc">Newest or largest first</option>
              <option value="asc">Oldest or smallest first</option>
            </select>
          </div>
          <div className="col-6 col-md-3">
            <label htmlFor="f-size" className="form-label small mb-1">Rows per page</label>
            <select
              id="f-size"
              className="form-select"
              value={size}
              onChange={(event) => updateParams({ size: Number(event.target.value) === DEFAULT_SIZE ? '' : event.target.value })}
            >
              {PAGE_SIZES.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </div>
          <div className="col-6 col-md-3">
            <button type="button" className="btn btn-outline-secondary w-100" onClick={clearFilters} disabled={!hasFilters}>
              Clear filters
            </button>
          </div>
        </div>
      </form>

      {deleteError && (
        <ErrorAlert message={getErrorMessage(deleteError)} fieldErrors={getFieldErrors(deleteError)} />
      )}

      {/* ---------- result ---------- */}
      {loading && <Spinner />}

      {!loading && loadError && (
        <ErrorAlert message={getErrorMessage(loadError)} fieldErrors={getFieldErrors(loadError)} />
      )}

      {!loading && !loadError && data && content.length === 0 && (
        pastLastPage ? (
          <EmptyState
            title="This page has no results"
            description="Go back to the first page."
            actionLabel="Go to the first page"
            onAction={() => goToPage(0)}
          />
        ) : hasFilters ? (
          <EmptyState
            title="No transactions match your filters"
            description="Change or clear the filters to see more."
            actionLabel="Clear filters"
            onAction={clearFilters}
          />
        ) : (
          <EmptyState
            title={fixedType ? `No ${TITLES[fixedType].toLowerCase()} yet` : 'No transactions yet'}
            description="Add one by hand, or upload a bank statement."
            actionLabel="Add transaction"
            onAction={handleAdd}
          />
        )
      )}

      {!loading && !loadError && data && content.length > 0 && (
        <>
          <div className="table-responsive">
            <table className="table table-hover align-middle" data-testid="transactions-table">
              <thead>
                <tr>
                  <th scope="col">Date</th>
                  <th scope="col">Description</th>
                  <th scope="col">Category</th>
                  <th scope="col">Type</th>
                  <th scope="col" className="text-end">Amount</th>
                  <th scope="col">Source</th>
                  <th scope="col">Payment method</th>
                  <th scope="col"><span className="visually-hidden">Actions</span></th>
                </tr>
              </thead>
              <tbody>
                {content.map((item) => {
                  const isCredit = item.type === 'CREDIT';
                  return (
                    <tr key={item.id} data-testid={`transaction-row-${item.id}`}>
                      <td className="text-nowrap">{formatDate(item.transactionDate)}</td>
                      <td className="text-break">{item.description}</td>
                      <td>{item.categoryName}</td>
                      <td>
                        <span className={`badge ${isCredit ? 'text-bg-success' : 'text-bg-danger'}`}>
                          {isCredit ? '+ Credit' : '- Debit'}
                        </span>
                      </td>
                      <td className={`text-end text-nowrap ${isCredit ? 'text-success' : 'text-danger'}`}>
                        {isCredit ? '+' : '-'} {formatMoney(item.amount, currency)}
                      </td>
                      <td>
                        <span className={`badge ${item.source === 'MANUAL' ? 'text-bg-primary' : 'text-bg-secondary'}`}>
                          {item.source === 'MANUAL' ? 'Manual' : 'Bank'}
                        </span>
                      </td>
                      <td>{item.paymentMethod || '-'}</td>
                      <td className="text-nowrap">
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-primary me-1"
                          onClick={() => navigate(`/transactions/${item.id}/edit`)}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-danger"
                          onClick={() => {
                            setDeleteError(null);
                            setToDelete(item);
                          }}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="d-flex flex-wrap justify-content-between align-items-center gap-2">
            <span className="text-muted small" data-testid="transactions-summary">
              Showing {content.length} of {data.totalElements} transactions
            </span>
            <nav aria-label="Transaction pages">
              <ul className="pagination mb-0">
                <li className={`page-item ${page === 0 ? 'disabled' : ''}`}>
                  <button type="button" className="page-link" disabled={page === 0} onClick={() => goToPage(page - 1)}>
                    Previous
                  </button>
                </li>
                <li className="page-item disabled">
                  <span className="page-link">Page {data.page + 1} of {data.totalPages}</span>
                </li>
                <li className={`page-item ${data.last ? 'disabled' : ''}`}>
                  <button type="button" className="page-link" disabled={data.last} onClick={() => goToPage(page + 1)}>
                    Next
                  </button>
                </li>
              </ul>
            </nav>
          </div>
        </>
      )}

      <ConfirmDialog
        open={Boolean(toDelete)}
        title="Delete transaction"
        busy={deleting}
        confirmLabel="Delete"
        message={
          toDelete && (
            <>
              Delete <strong>{toDelete.description}</strong> ({formatMoney(toDelete.amount, currency)} on{' '}
              {formatDate(toDelete.transactionDate)})? This cannot be undone.
              {toDelete.source === 'BANK_STATEMENT' && (
                <span className="d-block mt-2 text-muted">
                  This transaction was imported from a bank statement. Deleting it makes this app differ from your bank record.
                </span>
              )}
            </>
          )
        }
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </div>
  );
}