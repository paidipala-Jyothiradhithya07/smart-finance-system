export function Spinner({ label = 'Loading...' }) {
  return (
    <div className="d-flex justify-content-center my-4" data-testid="loading-spinner">
      <div className="spinner-border" role="status">
        <span className="visually-hidden">{label}</span>
      </div>
    </div>
  );
}

export default Spinner;