import { Navigate, useLocation, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

export function ProtectedRoute({ children }) {
  const auth = useAuth();
  const location = useLocation();

  const loading = auth?.loading;
  const user = auth?.user;
  const sessionExpired = auth?.sessionExpired;

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <Navigate
        to="/login"
        state={{ from: location, expired: sessionExpired }}
        replace
      />
    );
  }

  return children ? children : <Outlet />;
}

export default ProtectedRoute;
