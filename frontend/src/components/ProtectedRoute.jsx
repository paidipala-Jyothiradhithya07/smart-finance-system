import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../auth/AuthContext';

// ProtectedRoute ensures that the user is authenticated before rendering the component.
// If not authenticated, redirects to /login preserving the intended location.
export default function ProtectedRoute({ children }) {
  const { token, loading } = React.useContext(AuthContext);
  const location = useLocation();

  if (loading) {
    // Show a simple loading indicator while auth state is being determined.
    return <div className="spinner-border" role="status"><span className="visually-hidden">Loading...</span></div>;
  }

  if (!token) {
    // Not authenticated – redirect to login, storing the original location for post-login navigation.
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}
