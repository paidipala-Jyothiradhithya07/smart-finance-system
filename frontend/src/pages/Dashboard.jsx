import { useContext } from 'react';
import { AuthContext } from '../auth/AuthContext';

export default function Dashboard() {
  const { user } = useContext(AuthContext);

  return (
    <div className="container py-4">
      <h1 className="h3 mb-3">Dashboard</h1>
      <div className="card">
        <div className="card-body">
          <p className="mb-1">Welcome{user?.name ? `, ${user.name}` : ''}.</p>
          <p className="text-muted mb-0">
            The dashboard cards and recent transactions will appear here in Stage 2.
          </p>
        </div>
      </div>
    </div>
  );
}