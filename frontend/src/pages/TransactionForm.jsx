import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { createTransaction, getTransaction, updateTransaction } from '../api/transactions';
import { getCategories } from '../api/categories';
import { getErrorMessage, getFieldErrors } from '../utils/errors';
import Spinner from '../components/Spinner';
import ErrorAlert from '../components/ErrorAlert';

const AMOUNT_PATTERN = /^\d{1,13}(\.\d{1,2})?$/;
// The input names the server uses in its fieldErrors
const FIELD_NAMES = ['type', 'categoryId', 'transactionDate', 'description', 'amount', 'paymentMethod', 'referenceNo'];

function pad(number) {
  return String(number).padStart(2, '0');
}

// Today as yyyy-MM-dd in the browser's time zone (built from parts, never parsed from text)
function todayIso() {
  const now = new Date();
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

function initialForm(transaction, initialType) {
  if (transaction) {
    return {
      type: transaction.type,
      categoryId: String(transaction.categoryId),
      transactionDate: transaction.transactionDate,
      description: transaction.description ?? '',
      amount: String(transaction.amount), // kept as text from now on
      paymentMethod: transaction.paymentMethod ?? '',
      referenceNo: transaction.referenceNo ?? '',
    };
  }
  return {
    type: initialType,
    categoryId: '',
    transactionDate: todayIso(),
    description: '',
    amount: '',
    paymentMethod: '',
    referenceNo: '',
  };
}

// Checks that run in the browser. The server checks everything again.
function validate(form, today) {
  const problems = {};

  if (!form.transactionDate) {
    problems.transactionDate = 'Date is required.';
  } else if (form.transactionDate > today) {
    problems.transactionDate = 'The date cannot be in the future.';
  }

  if (!form.categoryId) {
    problems.categoryId = 'Choose a category.';
  }

  const description = form.description.trim();
  if (!description) {
    problems.description = 'Description is required.';
  } else if (description.length > 500) {
    problems.description = 'Description must be at most 500 characters.';
  }

  const amount = form.amount.trim();
  if (!amount) {
    problems.amount = 'Amount is required.';
  } else if (!AMOUNT_PATTERN.test(amount)) {
    problems.amount = 'Use digits with up to 2 decimals, for example 1000 or 99.50.';
  } else if (!/[1-9]/.test(amount)) {
    problems.amount = 'Amount must be greater than zero.';
  }

  if (form.paymentMethod.trim().length > 30) {
    problems.paymentMethod = 'Payment method must be at most 30 characters.';
  }
  if (form.referenceNo.trim().length > 100) {
    problems.referenceNo = 'Reference number must be at most 100 characters.';
  }
  return problems;
}

// The only place where the typed amount becomes a number
function buildBody(form) {
  return {
    transactionDate: form.transactionDate,
    description: form.description.trim(),
    amount: Number(form.amount.trim()),
    type: form.type,
    categoryId: Number(form.categoryId),
    paymentMethod: form.paymentMethod.trim() || null,
    referenceNo: form.referenceNo.trim() || null,
  };
}

function FieldError({ id, message }) {
  if (!message) {
    return null;
  }
  return (
    <div id={id} className="invalid-feedback d-block">
      {message}
    </div>
  );
}

function TransactionFormBody({ transaction, initialType }) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const currency = user?.currency ?? 'INR';
  const isEdit = Boolean(transaction);
  const locked = transaction?.source === 'BANK_STATEMENT';

  const [today] = useState(() => todayIso());
  const [form, setForm] = useState(() => initialForm(transaction, initialType));
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // ---- categories follow the type: INCOME for Income, EXPENSE for Expense (both include "Other") ----
  const categoryType = form.type === 'CREDIT' ? 'INCOME' : 'EXPENSE';
  const [catResult, setCatResult] = useState({ key: null, list: [], error: null });

  useEffect(() => {
    let cancelled = false;
    getCategories(categoryType)
      .then((list) => {
        if (!cancelled) {
          setCatResult({ key: categoryType, list, error: null });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setCatResult({ key: categoryType, list: [], error });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [categoryType]);

  const categoriesLoading = catResult.key !== categoryType;
  const categories = categoriesLoading ? [] : catResult.list;
  const categoryError = categoriesLoading ? null : catResult.error;

  // ---- editing the form ----
  const clearError = (...names) =>
    setErrors((previous) => {
      if (!names.some((name) => previous[name])) {
        return previous;
      }
      const next = { ...previous };
      names.forEach((name) => delete next[name]);
      return next;
    });

  const setField = (name, value) => {
    setForm((previous) => ({ ...previous, [name]: value }));
    clearError(name);
  };

  const changeType = (type) => {
    if (type === form.type) {
      return;
    }
    // A category belongs to one kind of transaction, so the choice is cleared
    setForm((previous) => ({ ...previous, type, categoryId: '' }));
    clearError('type', 'categoryId');
  };

  // ---- saving ----
  const handleSubmit = async (event) => {
    event.preventDefault();
    if (submitting) {
      return;
    }
    setGeneralError(null);

    const problems = validate(form, today);
    if (Object.keys(problems).length > 0) {
      setErrors(problems);
      return;
    }
    setErrors({});
    setSubmitting(true);

    try {
      const body = buildBody(form);
      if (isEdit) {
        await updateTransaction(transaction.id, body);
      } else {
        await createTransaction(body);
      }
      navigate('/transactions', { replace: true });
    } catch (error) {
      setErrors(getFieldErrors(error) ?? {});
      setGeneralError(getErrorMessage(error));
      setSubmitting(false);
    }
  };

  // Server errors for inputs this form does not have still need to be shown somewhere
  const unplaced = Object.fromEntries(
    Object.entries(errors).filter(([name, message]) => message && !FIELD_NAMES.includes(name)),
  );

  const title = isEdit ? 'Edit transaction' : 'Add transaction';

  return (
    <div className="container py-4">
      <div className="row justify-content-center">
        <div className="col-lg-7">
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
            <h1 className="h3 mb-0">{title}</h1>
            {isEdit && (
              <span className={`badge ${locked ? 'text-bg-secondary' : 'text-bg-primary'}`}>
                {locked ? 'Imported from a bank statement' : 'Entered manually'}
              </span>
            )}
          </div>

          {locked && (
            <div className="alert alert-info" role="note">
              This transaction was imported from a bank statement, so its date, amount and type cannot be
              changed. You can change the category, description, payment method and reference.
            </div>
          )}

          <ErrorAlert message={generalError} fieldErrors={unplaced} />
          {categoryError && (
            <ErrorAlert message={`Categories could not be loaded. ${getErrorMessage(categoryError)}`} />
          )}

          <form onSubmit={handleSubmit} noValidate data-testid="transaction-form">
            <fieldset className="mb-3" disabled={locked || submitting}>
              <legend className="form-label fs-6">Type</legend>
              <div className="form-check form-check-inline">
                <input
                  className="form-check-input"
                  type="radio"
                  name="type"
                  id="tx-type-debit"
                  checked={form.type === 'DEBIT'}
                  onChange={() => changeType('DEBIT')}
                />
                <label className="form-check-label" htmlFor="tx-type-debit">Expense (debit)</label>
              </div>
              <div className="form-check form-check-inline">
                <input
                  className="form-check-input"
                  type="radio"
                  name="type"
                  id="tx-type-credit"
                  checked={form.type === 'CREDIT'}
                  onChange={() => changeType('CREDIT')}
                />
                <label className="form-check-label" htmlFor="tx-type-credit">Income (credit)</label>
              </div>
              <FieldError id="tx-type-error" message={errors.type} />
            </fieldset>

            <div className="mb-3">
              <label htmlFor="tx-category" className="form-label">Category</label>
              <select
                id="tx-category"
                className={`form-select ${errors.categoryId ? 'is-invalid' : ''}`}
                value={form.categoryId}
                onChange={(event) => setField('categoryId', event.target.value)}
                disabled={categoriesLoading || submitting}
                aria-invalid={Boolean(errors.categoryId)}
                aria-describedby={errors.categoryId ? 'tx-category-error' : undefined}
                data-testid="tx-category"
              >
                <option value="">{categoriesLoading ? 'Loading categories...' : 'Choose a category'}</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>{category.name}</option>
                ))}
              </select>
              <FieldError id="tx-category-error" message={errors.categoryId} />
            </div>

            <div className="row">
              <div className="col-md-6 mb-3">
                <label htmlFor="tx-date" className="form-label">Date</label>
                <input
                  id="tx-date"
                  type="date"
                  className={`form-control ${errors.transactionDate ? 'is-invalid' : ''}`}
                  value={form.transactionDate}
                  max={today}
                  onChange={(event) => setField('transactionDate', event.target.value)}
                  disabled={locked || submitting}
                  aria-invalid={Boolean(errors.transactionDate)}
                  aria-describedby={errors.transactionDate ? 'tx-date-error' : undefined}
                  data-testid="tx-date"
                />
                <FieldError id="tx-date-error" message={errors.transactionDate} />
              </div>

              <div className="col-md-6 mb-3">
                <label htmlFor="tx-amount" className="form-label">Amount ({currency})</label>
                <input
                  id="tx-amount"
                  type="text"
                  inputMode="decimal"
                  autoComplete="off"
                  maxLength={16}
                  className={`form-control ${errors.amount ? 'is-invalid' : ''}`}
                  value={form.amount}
                  onChange={(event) => setField('amount', event.target.value)}
                  disabled={locked || submitting}
                  aria-invalid={Boolean(errors.amount)}
                  aria-describedby={errors.amount ? 'tx-amount-error' : 'tx-amount-help'}
                  data-testid="tx-amount"
                />
                {!errors.amount && <div id="tx-amount-help" className="form-text">For example 1000 or 99.50</div>}
                <FieldError id="tx-amount-error" message={errors.amount} />
              </div>
            </div>

            <div className="mb-3">
              <label htmlFor="tx-description" className="form-label">Description</label>
              <input
                id="tx-description"
                type="text"
                maxLength={500}
                className={`form-control ${errors.description ? 'is-invalid' : ''}`}
                value={form.description}
                onChange={(event) => setField('description', event.target.value)}
                disabled={submitting}
                aria-invalid={Boolean(errors.description)}
                aria-describedby={errors.description ? 'tx-description-error' : undefined}
                data-testid="tx-description"
              />
              <FieldError id="tx-description-error" message={errors.description} />
            </div>

            <div className="row">
              <div className="col-md-6 mb-3">
                <label htmlFor="tx-payment" className="form-label">Payment method (optional)</label>
                <input
                  id="tx-payment"
                  type="text"
                  maxLength={30}
                  placeholder="UPI, Card, Cash..."
                  className={`form-control ${errors.paymentMethod ? 'is-invalid' : ''}`}
                  value={form.paymentMethod}
                  onChange={(event) => setField('paymentMethod', event.target.value)}
                  disabled={submitting}
                  aria-invalid={Boolean(errors.paymentMethod)}
                  aria-describedby={errors.paymentMethod ? 'tx-payment-error' : undefined}
                />
                <FieldError id="tx-payment-error" message={errors.paymentMethod} />
              </div>
              <div className="col-md-6 mb-3">
                <label htmlFor="tx-reference" className="form-label">Reference (optional)</label>
                <input
                  id="tx-reference"
                  type="text"
                  maxLength={100}
                  className={`form-control ${errors.referenceNo ? 'is-invalid' : ''}`}
                  value={form.referenceNo}
                  onChange={(event) => setField('referenceNo', event.target.value)}
                  disabled={submitting}
                  aria-invalid={Boolean(errors.referenceNo)}
                  aria-describedby={errors.referenceNo ? 'tx-reference-error' : undefined}
                />
                <FieldError id="tx-reference-error" message={errors.referenceNo} />
              </div>
            </div>

            <div className="d-flex gap-2">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={submitting || categoriesLoading}
                data-testid="tx-submit"
              >
                {submitting ? 'Saving...' : isEdit ? 'Save changes' : 'Add transaction'}
              </button>
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => navigate('/transactions')}
                disabled={submitting}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

// The route component: for "new" it renders the form at once, for "edit" it loads the transaction first
export default function TransactionForm() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const isEdit = id !== undefined;
  const initialType = searchParams.get('type') === 'CREDIT' ? 'CREDIT' : 'DEBIT';

  const [result, setResult] = useState({ key: null, data: null, error: null });

  useEffect(() => {
    if (!isEdit) {
      return undefined;
    }
    let cancelled = false;
    getTransaction(id)
      .then((data) => {
        if (!cancelled) {
          setResult({ key: id, data, error: null });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setResult({ key: id, data: null, error });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [id, isEdit]);

  if (!isEdit) {
    return <TransactionFormBody key={`new-${initialType}`} transaction={null} initialType={initialType} />;
  }

  if (result.key !== id) {
    return (
      <div className="container py-4">
        <Spinner />
      </div>
    );
  }

  if (result.error) {
    return (
      <div className="container py-4">
        <ErrorAlert message={getErrorMessage(result.error)} />
        <Link to="/transactions" className="btn btn-outline-secondary">
          Back to transactions
        </Link>
      </div>
    );
  }

  return <TransactionFormBody key={id} transaction={result.data} initialType="DEBIT" />;
}