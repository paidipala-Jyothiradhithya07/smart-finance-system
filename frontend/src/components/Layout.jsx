import React from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { AuthContext } from '../auth/AuthContext';

export default function Layout() {
  const { user, logout } = React.useContext(AuthContext);
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const linkClass = ({ isActive }) =>
    `nav-link${isActive ? ' active' : ''}`;

  return (
    <>
      <nav className="navbar navbar-expand-lg navbar-light bg-light">
        <div className="container-fluid">
          <NavLink className="navbar-brand" to="/dashboard">Smart Finance</NavLink>
          <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
            <span className="navbar-toggler-icon"></span>
          </button>
          <div className="collapse navbar-collapse" id="navbarNav">
            <ul className="navbar-nav me-auto mb-2 mb-lg-0">
              <li className="nav-item"><NavLink to="/dashboard" className={linkClass}>Dashboard</NavLink></li>
              <li className="nav-item"><NavLink to="/transactions" className={linkClass}>Transactions</NavLink></li>
              <li className="nav-item"><NavLink to="/income" className={linkClass}>Income</NavLink></li>
              <li className="nav-item"><NavLink to="/expenses" className={linkClass}>Expenses</NavLink></li>
              <li className="nav-item"><NavLink to="/upload" className={linkClass}>Upload</NavLink></li>
              <li className="nav-item"><NavLink to="/analysis" className={linkClass}>Analysis</NavLink></li>
              <li className="nav-item"><NavLink to="/settings" className={linkClass}>Settings</NavLink></li>
            </ul>
            <span className="navbar-text me-3" data-testid="navbar-username">
              {user?.name || ''}
            </span>
            <button className="btn btn-outline-danger" onClick={handleLogout} data-testid="navbar-logout">Logout</button>
          </div>
        </div>
      </nav>
      <div className="container mt-4"><Outlet /></div>
    </>
  );
}
