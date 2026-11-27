import { useState, useRef, useEffect } from 'react';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

export function Layout() {
  const [isNavOpen, setIsNavOpen] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const [hasUnread, setHasUnread] = useState(true);
  const notifRef = useRef(null);

  const auth = useAuth();
  const navigate = useNavigate();

  const user = auth?.user;
  const logout = auth?.logout;

  const toggleNav = () => {
    setIsNavOpen((prev) => !prev);
  };

  const handleLogout = async () => {
    if (logout) {
      await logout();
    }
    navigate('/login');
  };

  // Close notifications dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (notifRef.current && !notifRef.current.contains(event.target)) {
        setShowNotifications(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const toggleNotifications = () => {
    setShowNotifications((prev) => !prev);
    if (hasUnread) {
      setHasUnread(false);
    }
  };

  const userInitial = user?.name ? user.name.charAt(0).toUpperCase() : 'U';

  return (
    <div className="d-flex flex-column min-vh-100">
      <nav className="navbar navbar-expand-lg navbar-dark premium-navbar">
        <div className="container">
          <Link className="navbar-brand d-flex align-items-center" to="/dashboard">
            <span className="brand-badge">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <line x1="12" y1="1" x2="12" y2="23"></line>
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
              </svg>
            </span>
            <span className="brand-text">
              Smart<span className="brand-text-accent">Finance</span>
            </span>
          </Link>

          <button
            className="navbar-toggler border-0 shadow-none"
            type="button"
            aria-controls="navbarSupportedContent"
            aria-expanded={isNavOpen}
            aria-label="Toggle navigation"
            onClick={toggleNav}
          >
            <span className="navbar-toggler-icon"></span>
          </button>

          <div
            className={`collapse navbar-collapse ${isNavOpen ? 'show' : ''}`}
            id="navbarSupportedContent"
          >
            <ul className="navbar-nav me-auto mb-2 mb-lg-0 ms-lg-3">
              <li className="nav-item">
                <Link
                  className="nav-link nav-link-custom"
                  to="/dashboard"
                  onClick={() => setIsNavOpen(false)}
                >
                  Dashboard
                </Link>
              </li>
              <li className="nav-item">
                <Link
                  className="nav-link nav-link-custom"
                  to="/transactions"
                  onClick={() => setIsNavOpen(false)}
                >
                  Transactions
                </Link>
              </li>
              <li className="nav-item">
                <Link
                  className="nav-link nav-link-custom"
                  to="/income"
                  onClick={() => setIsNavOpen(false)}
                >
                  Income
                </Link>
              </li>
              <li className="nav-item">
                <Link
                  className="nav-link nav-link-custom"
                  to="/expenses"
                  onClick={() => setIsNavOpen(false)}
                >
                  Expenses
                </Link>
              </li>
              <li className="nav-item">
                <Link
                  className="nav-link nav-link-custom"
                  to="/statements"
                  onClick={() => setIsNavOpen(false)}
                >
                  Bank Statements
                </Link>
              </li>
              <li className="nav-item">
                <Link
                  className="nav-link nav-link-custom"
                  to="/analysis"
                  onClick={() => setIsNavOpen(false)}
                >
                  Analysis
                </Link>
              </li>
            </ul>

            <div className="d-flex align-items-center gap-3 pt-2 pt-lg-0 border-top border-lg-0 border-secondary">
              {/* Dynamic Interactive Notifications Bell */}
              <div className="position-relative" ref={notifRef}>
                <button
                  type="button"
                  className="icon-btn-ghost"
                  onClick={toggleNotifications}
                  title="Notifications"
                  aria-label="Notifications"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                    <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                  </svg>
                  {hasUnread && <span className="notification-badge-dot"></span>}
                </button>

                {showNotifications && (
                  <div className="notification-popover animate-fade-in shadow-lg">
                    <div className="d-flex justify-content-between align-items-center mb-2 pb-2 border-bottom">
                      <span className="fw-bold small text-dark">Notifications</span>
                      <span className="badge bg-primary bg-opacity-10 text-primary small">System</span>
                    </div>
                    <div className="d-flex flex-column gap-2 small">
                      <div className="p-2 bg-light rounded-2">
                        <div className="fw-semibold text-dark">Session Active</div>
                        <div className="text-muted" style={{ fontSize: '0.78rem' }}>
                          Signed in securely as {user?.email || 'User'}.
                        </div>
                      </div>
                      <div className="p-2 bg-light rounded-2">
                        <div className="fw-semibold text-dark">Currency Profile</div>
                        <div className="text-muted" style={{ fontSize: '0.78rem' }}>
                          Default currency configured to {user?.currency || 'USD'}.
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* User Profile Badge */}
              {user?.name && (
                <div className="d-flex align-items-center gap-2">
                  <span className="user-avatar-badge" title={user.name}>
                    {userInitial}
                  </span>
                  <div className="d-flex flex-column">
                    <span className="text-white fw-semibold small lh-1">{user.name}</span>
                    {user.currency && (
                      <span className="text-light small text-opacity-75 lh-1 mt-1" style={{ fontSize: '0.75rem' }}>
                        {user.currency}
                      </span>
                    )}
                  </div>
                </div>
              )}

              {/* Logout Button */}
              <button
                type="button"
                className="btn btn-outline-light btn-sm px-3 rounded-pill d-inline-flex align-items-center gap-1"
                data-testid="nav-logout"
                onClick={handleLogout}
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                  <polyline points="16 17 21 12 16 7"></polyline>
                  <line x1="21" y1="12" x2="9" y2="12"></line>
                </svg>
                Logout
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="container my-4 flex-grow-1">
        <Outlet />
      </main>

      <footer className="border-top py-3 text-center text-muted small bg-white mt-auto">
        <div className="container d-flex flex-column flex-sm-row justify-content-between align-items-center gap-2">
          <span>Smart Finance System &bull; Enterprise Financial Management</span>
          <span className="badge bg-light text-secondary border">API v1 Connected</span>
        </div>
      </footer>
    </div>
  );
}

export default Layout;
