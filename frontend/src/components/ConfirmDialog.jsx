import { useEffect } from 'react';

// Plain Bootstrap markup (no react-bootstrap). Escape closes it.
export function ConfirmDialog({
  open,
  title = 'Please confirm',
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  busy = false,
  onConfirm,
  onCancel,
}) {
  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const onKey = (event) => {
      if (event.key === 'Escape') {
        onCancel();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, onCancel]);

  if (!open) {
    return null;
  }

  return (
    <>
      <div className="modal-backdrop fade show" />
      <div
        className="modal fade show d-block"
        tabIndex="-1"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        data-testid="confirm-dialog"
      >
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header">
              <h2 className="modal-title h5" id="confirm-dialog-title">{title}</h2>
            </div>
            <div className="modal-body">{message}</div>
            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={busy} autoFocus>
                {cancelLabel}
              </button>
              <button type="button" className="btn btn-danger" onClick={onConfirm} disabled={busy}>
                {busy ? 'Working...' : confirmLabel}
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

export default ConfirmDialog;