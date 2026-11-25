import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="container py-5 text-center">
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-6">
          <div className="premium-card p-5">
            <div
              className="mx-auto mb-3 d-flex align-items-center justify-content-center rounded-circle bg-danger bg-opacity-10 text-danger"
              style={{ width: '80px', height: '80px' }}
            >
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
            </div>
            <h1 className="display-4 fw-bold text-dark mb-2">404</h1>
            <h2 className="h4 fw-semibold text-secondary mb-3">Page Not Found</h2>
            <p className="text-muted mb-4">
              The page you are looking for does not exist or may have been moved.
            </p>
            <Link to="/dashboard" className="btn btn-primary px-4 py-2">
              Back to Dashboard
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
