export function ErrorAlert({ message, fieldErrors }) {
  const entries = fieldErrors ? Object.entries(fieldErrors) : [];
  if (!message && entries.length === 0) {
    return null;
  }

  return (
    <div className="alert alert-danger" role="alert" data-testid="error-alert">
      {message && <p className={entries.length > 0 ? 'mb-1' : 'mb-0'}>{message}</p>}
      {entries.length > 0 && (
        <ul className="mb-0">
          {entries.map(([field, text]) => (
            <li key={field}>
              <strong>{field}:</strong> {text}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default ErrorAlert;