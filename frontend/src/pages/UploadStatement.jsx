import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { deleteStatement, getStatements, previewStatements, uploadStatements } from '../api/statements';
import { getErrorMessage, getFieldErrors } from '../utils/errors';
import { formatDate, formatMoney } from '../utils/format';
import Spinner from '../components/Spinner';
import ErrorAlert from '../components/ErrorAlert';
import EmptyState from '../components/EmptyState';
import ConfirmDialog from '../components/ConfirmDialog';

const MAX_FILES = 5;
const MAX_BYTES = 5 * 1024 * 1024;
const ALLOWED_EXTENSIONS = ['csv', 'xlsx', 'xls'];
const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];
const ROLE_LABELS = {
  date: 'Date',
  description: 'Description',
  debit: 'Debit',
  credit: 'Credit',
  amount: 'Amount',
  balance: 'Balance',
  type: 'Type',
  reference: 'Reference',
};
const IMPORT_STATUS = {
  IMPORTED: { label: 'Imported', style: 'text-bg-success' },
  IMPORTED_WITH_ERRORS: { label: 'Imported with errors', style: 'text-bg-warning' },
  NO_NEW_TRANSACTIONS: { label: 'No new transactions', style: 'text-bg-info' },
  ALREADY_IMPORTED: { label: 'Already imported', style: 'text-bg-secondary' },
  FAILED: { label: 'Failed', style: 'text-bg-danger' },
};
const HISTORY_STATUS = {
  COMPLETED: { label: 'Completed', style: 'text-bg-success' },
  COMPLETED_WITH_ERRORS: { label: 'Completed with errors', style: 'text-bg-warning' },
};

// ---------- small helpers (no arithmetic on money anywhere) ----------

function extensionOf(name) {
  const dot = name.lastIndexOf('.');
  return dot < 0 ? '' : name.slice(dot + 1).toLowerCase();
}

// "2026-01" becomes "January 2026". Built from the text parts, never parsed as a date.
function monthLabel(value) {
  const [year, month] = String(value).split('-');
  const name = MONTH_NAMES[Number(month) - 1];
  return name ? `${name} ${year}` : String(value);
}

function fileSize(file) {
  return `${Math.max(1, Math.round(file.size / 1024))} KB`;
}

// Quick checks for the user's convenience. The server decides again.
function checkFiles(list) {
  const accepted = [];
  const notices = [];
  list.forEach((file) => {
    const extension = extensionOf(file.name);
    if (extension === 'pdf') {
      notices.push(`${file.name}: PDF statements are not supported yet. Please upload a CSV or Excel file.`);
    } else if (!ALLOWED_EXTENSIONS.includes(extension)) {
      notices.push(`${file.name}: unsupported file type. Use .csv, .xlsx or .xls.`);
    } else if (file.size === 0) {
      notices.push(`${file.name}: the file is empty.`);
    } else if (file.size > MAX_BYTES) {
      notices.push(`${file.name}: larger than 5 MB.`);
    } else if (accepted.length >= MAX_FILES) {
      notices.push(`${file.name}: skipped, at most ${MAX_FILES} files can be uploaded at once.`);
    } else {
      accepted.push(file);
    }
  });
  return { accepted, notices };
}

function period(start, end) {
  if (!start || !end) {
    return null;
  }
  return `${formatDate(start)} to ${formatDate(end)}`;
}

// ---------- small display pieces ----------

function TypeBadge({ type }) {
  const credit = type === 'CREDIT';
  return (
    <span className={`badge ${credit ? 'text-bg-success' : 'text-bg-danger'}`}>
      {credit ? '+ Credit' : '- Debit'}
    </span>
  );
}

function Months({ months }) {
  if (!months || months.length === 0) {
    return null;
  }
  return (
    <p className="mb-2">
      <strong>Months:</strong>{' '}
      {months.map((item) => (
        <span key={item.month} className="badge text-bg-light border me-1">
          {monthLabel(item.month)}: {item.transactionCount}
        </span>
      ))}
    </p>
  );
}

function Warnings({ warnings }) {
  if (!warnings || warnings.length === 0) {
    return null;
  }
  return (
    <div className="alert alert-warning py-2 mb-2" role="status">
      <strong>Warnings</strong>
      <ul className="mb-0">
        {warnings.map((warning, index) => (
          <li key={index}>{warning}</li>
        ))}
      </ul>
    </div>
  );
}

function RowErrors({ errors, truncated }) {
  if (!errors || errors.length === 0) {
    return null;
  }
  return (
    <div className="mt-3">
      <h4 className="h6">Rows with problems (not imported)</h4>
      <div className="table-responsive">
        <table className="table table-sm table-bordered mb-1">
          <thead>
            <tr>
              <th scope="col">Row</th>
              <th scope="col">Column</th>
              <th scope="col">Problem</th>
            </tr>
          </thead>
          <tbody>
            {errors.map((error, index) => (
              <tr key={`${error.row}-${error.column}-${index}`}>
                <td>{error.row}</td>
                <td>{error.column}</td>
                <td className="text-break">{error.message}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {truncated && <p className="text-muted small mb-0">Only the first {errors.length} problems are shown.</p>}
    </div>
  );
}

function Counts({ items }) {
  return (
    <ul className="list-inline mb-2">
      {items.map((item) => (
        <li key={item.label} className="list-inline-item me-3">
          <span className="text-muted">{item.label}:</span> <strong>{item.value}</strong>
        </li>
      ))}
    </ul>
  );
}

function PreviewCard({ file, currency }) {
  const failed = file.status !== 'OK';
  return (
    <div className="card mb-3" data-testid={`preview-${file.fileName}`}>
      <div className="card-header d-flex flex-wrap justify-content-between gap-2">
        <strong className="text-break">{file.fileName}</strong>
        <span className={`badge ${failed ? 'text-bg-danger' : 'text-bg-success'}`}>
          {failed ? 'Cannot be read' : `Readable (${file.fileType})`}
        </span>
      </div>
      <div className="card-body">
        {failed ? (
          <div className="alert alert-danger mb-0" role="alert">{file.fileError}</div>
        ) : (
          <>
            <Counts
              items={[
                { label: 'Header row', value: file.headerRow },
                { label: 'Data rows', value: file.totalDataRows },
                { label: 'Valid', value: file.validRows },
                { label: 'Invalid', value: file.invalidRows },
                { label: 'Skipped', value: file.skippedRows },
              ]}
            />
            {period(file.periodStart, file.periodEnd) && (
              <p className="mb-2"><strong>Period:</strong> {period(file.periodStart, file.periodEnd)}</p>
            )}
            <Months months={file.monthsDetected} />

            {file.detectedColumns && Object.keys(file.detectedColumns).length > 0 && (
              <p className="mb-2">
                <strong>Columns found:</strong>{' '}
                {Object.entries(file.detectedColumns).map(([role, header]) => (
                  <span key={role} className="badge text-bg-light border me-1">
                    {ROLE_LABELS[role] ?? role}: {header}
                  </span>
                ))}
              </p>
            )}

            <Warnings warnings={file.warnings} />
            <RowErrors errors={file.errors} truncated={file.errorsTruncated} />

            {file.sample && file.sample.length > 0 && (
              <div className="mt-3">
                <h4 className="h6">First rows (sample)</h4>
                <div className="table-responsive">
                  <table className="table table-sm mb-0">
                    <thead>
                      <tr>
                        <th scope="col">Row</th>
                        <th scope="col">Date</th>
                        <th scope="col">Description</th>
                        <th scope="col">Type</th>
                        <th scope="col" className="text-end">Amount</th>
                        <th scope="col" className="text-end">Balance</th>
                      </tr>
                    </thead>
                    <tbody>
                      {file.sample.map((row) => (
                        <tr key={row.row}>
                          <td>{row.row}</td>
                          <td className="text-nowrap">{formatDate(row.date)}</td>
                          <td className="text-break">{row.description}</td>
                          <td><TypeBadge type={row.type} /></td>
                          <td className="text-end text-nowrap">{formatMoney(row.amount, currency)}</td>
                          <td className="text-end text-nowrap">
                            {row.balance === null || row.balance === undefined
                              ? 'Not available'
                              : formatMoney(row.balance, currency)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function ImportCard({ file, currency }) {
  const status = IMPORT_STATUS[file.status] ?? { label: file.status, style: 'text-bg-secondary' };
  const showCounts = file.status !== 'FAILED' && file.status !== 'ALREADY_IMPORTED';
  return (
    <div className="card mb-3" data-testid={`import-${file.fileName}`}>
      <div className="card-header d-flex flex-wrap justify-content-between gap-2">
        <strong className="text-break">{file.fileName}</strong>
        <span className={`badge ${status.style}`} data-testid="import-file-status">{status.label}</span>
      </div>
      <div className="card-body">
        {file.message && <p className="mb-2">{file.message}</p>}

        {showCounts && (
          <Counts
            items={[
              { label: 'Rows found', value: file.transactionsDetected },
              { label: 'Imported', value: file.imported },
              { label: 'Duplicates skipped', value: file.duplicates },
              { label: 'Invalid', value: file.invalid },
              { label: 'Skipped', value: file.skipped },
            ]}
          />
        )}
        {period(file.periodStart, file.periodEnd) && (
          <p className="mb-2"><strong>Period:</strong> {period(file.periodStart, file.periodEnd)}</p>
        )}
        <Months months={file.monthsDetected} />
        <Warnings warnings={file.warnings} />
        <RowErrors errors={file.errors} truncated={file.errorsTruncated} />

        {file.duplicateRows && file.duplicateRows.length > 0 && (
          <div className="mt-3">
            <h4 className="h6">Duplicates (already in your account, skipped)</h4>
            <div className="table-responsive">
              <table className="table table-sm mb-1">
                <thead>
                  <tr>
                    <th scope="col">Row</th>
                    <th scope="col">Date</th>
                    <th scope="col">Description</th>
                    <th scope="col">Type</th>
                    <th scope="col" className="text-end">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {file.duplicateRows.map((row) => (
                    <tr key={`${row.row}-${row.date}`}>
                      <td>{row.row}</td>
                      <td className="text-nowrap">{formatDate(row.date)}</td>
                      <td className="text-break">{row.description}</td>
                      <td><TypeBadge type={row.type} /></td>
                      <td className="text-end text-nowrap">{formatMoney(row.amount, currency)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {file.duplicatesTruncated && (
              <p className="text-muted small mb-0">Only the first {file.duplicateRows.length} duplicates are shown.</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// ---------- the page ----------

export default function UploadStatement() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const currency = user?.currency ?? 'INR';

  const [files, setFiles] = useState([]);
  const [notices, setNotices] = useState([]);
  const [inputKey, setInputKey] = useState(0);
  const [busy, setBusy] = useState(null); // null, 'preview' or 'import'
  const [previewResult, setPreviewResult] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const [actionError, setActionError] = useState(null);

  // ---- history ----
  const [historyReload, setHistoryReload] = useState(0);
  const [history, setHistory] = useState({ key: null, data: null, error: null });

  useEffect(() => {
    let cancelled = false;
    getStatements()
      .then((data) => {
        if (!cancelled) {
          setHistory({ key: historyReload, data, error: null });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setHistory({ key: historyReload, data: null, error });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [historyReload]);

  const historyLoading = history.key !== historyReload;

  // ---- choosing files ----
  const handleChoose = (event) => {
    const { accepted, notices: problems } = checkFiles(Array.from(event.target.files ?? []));
    setFiles(accepted);
    setNotices(problems);
    setPreviewResult(null);
    setImportResult(null);
    setActionError(null);
  };

  const handleClear = () => {
    setFiles([]);
    setNotices([]);
    setPreviewResult(null);
    setImportResult(null);
    setActionError(null);
    setInputKey((key) => key + 1); // empties the native file box, so the same files can be chosen again
  };

  // ---- preview and import ----
  const canImport = Boolean(previewResult) && previewResult.filesReadable > 0 && !importResult;

  const runPreview = async () => {
    if (busy || files.length === 0) {
      return;
    }
    setBusy('preview');
    setActionError(null);
    setImportResult(null);
    try {
      setPreviewResult(await previewStatements(files));
    } catch (error) {
      setPreviewResult(null);
      setActionError(error);
    } finally {
      setBusy(null);
    }
  };

  const runImport = async () => {
    if (busy || !canImport) {
      return;
    }
    setBusy('import');
    setActionError(null);
    try {
      setImportResult(await uploadStatements(files));
      setHistoryReload((count) => count + 1);
    } catch (error) {
      setActionError(error);
    } finally {
      setBusy(null);
    }
  };

  // ---- undo an import ----
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const confirmDelete = async () => {
    if (!toDelete) {
      return;
    }
    setDeleting(true);
    try {
      await deleteStatement(toDelete.id);
      setToDelete(null);
      setHistoryReload((count) => count + 1);
    } catch (error) {
      setToDelete(null);
      setDeleteError(error);
    } finally {
      setDeleting(false);
    }
  };

  const statements = history.data ?? [];

  return (
    <div className="container py-4">
      <h1 className="h3 mb-3">Upload bank statement</h1>

      {/* ---------- choose and send ---------- */}
      <div className="card card-body mb-3">
        <label htmlFor="statement-files" className="form-label">Statement files</label>
        <input
          id="statement-files"
          key={inputKey}
          type="file"
          className="form-control"
          multiple
          accept=".csv,.xlsx,.xls"
          onChange={handleChoose}
          disabled={Boolean(busy)}
          data-testid="upload-file-input"
        />
        <div className="form-text">
          Up to {MAX_FILES} files at once (hold Ctrl to choose several), CSV or Excel (.xlsx, .xls), 5 MB each.
          PDF is not supported yet.
        </div>

        {notices.length > 0 && (
          <div className="alert alert-warning mt-3 mb-0" role="alert">
            <ul className="mb-0">
              {notices.map((notice) => (
                <li key={notice}>{notice}</li>
              ))}
            </ul>
          </div>
        )}

        {files.length > 0 && (
          <ul className="list-group mt-3" data-testid="selected-files">
            {files.map((file, index) => (
              <li key={`${file.name}-${index}`} className="list-group-item d-flex justify-content-between">
                <span className="text-break">{file.name}</span>
                <span className="text-muted ms-3 text-nowrap">{fileSize(file)}</span>
              </li>
            ))}
          </ul>
        )}

        <div className="d-flex flex-wrap gap-2 mt-3">
          <button
            type="button"
            className="btn btn-outline-primary"
            onClick={runPreview}
            disabled={Boolean(busy) || files.length === 0}
            data-testid="upload-preview-button"
          >
            {busy === 'preview' ? 'Reading...' : 'Preview'}
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={runImport}
            disabled={Boolean(busy) || !canImport}
            data-testid="upload-import-button"
          >
            {busy === 'import' ? 'Importing...' : 'Import'}
          </button>
          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={handleClear}
            disabled={Boolean(busy) || (files.length === 0 && notices.length === 0)}
          >
            Clear
          </button>
        </div>
        <p className="text-muted small mt-2 mb-0">
          Preview only reads and checks the files. Nothing is saved until you click Import.
        </p>
      </div>

      {actionError && (
        <ErrorAlert message={getErrorMessage(actionError)} fieldErrors={getFieldErrors(actionError)} />
      )}
      {busy && <Spinner label={busy === 'preview' ? 'Reading the files...' : 'Importing...'} />}

      {/* ---------- import report ---------- */}
      {importResult && (
        <section aria-label="Import report" className="mb-4">
          <div
            className={`alert ${importResult.filesImported > 0 ? 'alert-success' : 'alert-secondary'}`}
            role="status"
            data-testid="import-message"
          >
            {importResult.message}
          </div>
          <Counts
            items={[
              { label: 'Files received', value: importResult.filesReceived },
              { label: 'Files imported', value: importResult.filesImported },
              { label: 'Transactions imported', value: importResult.transactionsImported },
              { label: 'Duplicates skipped', value: importResult.duplicatesSkipped },
              { label: 'Invalid rows', value: importResult.invalidRows },
              { label: 'Months', value: importResult.monthsImported },
            ]}
          />
          {importResult.files.map((file, index) => (
            <ImportCard key={`${file.fileName}-${index}`} file={file} currency={currency} />
          ))}
          <button type="button" className="btn btn-success" onClick={() => navigate('/analysis')}>
            View monthly analysis
          </button>
        </section>
      )}

      {/* ---------- preview report ---------- */}
      {!importResult && previewResult && (
        <section aria-label="Preview" className="mb-4">
          <h2 className="h5">Preview</h2>
          <p className="text-muted">
            {previewResult.filesReadable} of {previewResult.filesReceived} file(s) can be read. Check the numbers,
            then click Import.
          </p>
          {previewResult.files.map((file, index) => (
            <PreviewCard key={`${file.fileName}-${index}`} file={file} currency={currency} />
          ))}
        </section>
      )}

      {/* ---------- history ---------- */}
      <section aria-label="Statement history">
        <h2 className="h5">Statement history</h2>
        {deleteError && (
          <ErrorAlert message={getErrorMessage(deleteError)} fieldErrors={getFieldErrors(deleteError)} />
        )}
        {historyLoading && <Spinner />}
        {!historyLoading && history.error && (
          <ErrorAlert message={getErrorMessage(history.error)} fieldErrors={getFieldErrors(history.error)} />
        )}
        {!historyLoading && !history.error && statements.length === 0 && (
          <EmptyState title="No statements yet" description="Imported statements will appear here." />
        )}
        {!historyLoading && !history.error && statements.length > 0 && (
          <div className="table-responsive">
            <table className="table table-hover align-middle" data-testid="statement-history">
              <thead>
                <tr>
                  <th scope="col">File</th>
                  <th scope="col">Status</th>
                  <th scope="col">Period</th>
                  <th scope="col" className="text-end">Imported</th>
                  <th scope="col" className="text-end">Duplicates</th>
                  <th scope="col" className="text-end">Invalid</th>
                  <th scope="col">Uploaded</th>
                  <th scope="col"><span className="visually-hidden">Actions</span></th>
                </tr>
              </thead>
              <tbody>
                {statements.map((statement) => {
                  const status = HISTORY_STATUS[statement.status] ?? {
                    label: statement.status,
                    style: 'text-bg-secondary',
                  };
                  return (
                    <tr key={statement.id} data-testid={`statement-row-${statement.id}`}>
                      <td className="text-break">{statement.fileName}</td>
                      <td><span className={`badge ${status.style}`}>{status.label}</span></td>
                      <td className="text-nowrap">{period(statement.periodStart, statement.periodEnd) ?? '-'}</td>
                      <td className="text-end">{statement.importedCount}</td>
                      <td className="text-end">{statement.duplicateCount}</td>
                      <td className="text-end">{statement.invalidCount}</td>
                      <td className="text-nowrap">
                        {statement.uploadedAt ? formatDate(statement.uploadedAt.split('T')[0]) : '-'}
                      </td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-danger"
                          onClick={() => {
                            setDeleteError(null);
                            setToDelete(statement);
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
        )}
      </section>

      <ConfirmDialog
        open={Boolean(toDelete)}
        title="Delete statement"
        busy={deleting}
        confirmLabel="Delete statement"
        message={
          toDelete && (
            <>
              Delete <strong>{toDelete.fileName}</strong>? All {toDelete.importedCount} transaction(s) imported
              from this statement will be removed from your account. This undoes the import and cannot be undone.
            </>
          )
        }
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </div>
  );
}