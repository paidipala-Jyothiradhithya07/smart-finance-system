export function EmptyState({ title, description, actionLabel, onAction }) {
  return (
    <div className="text-center my-5" data-testid="empty-state">
      <h2 className="h5">{title}</h2>
      {description && <p className="text-muted">{description}</p>}
      {actionLabel && onAction && (
        <button type="button" className="btn btn-primary" onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}

export default EmptyState;