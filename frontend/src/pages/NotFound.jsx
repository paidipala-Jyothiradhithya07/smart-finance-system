import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="container py-5 text-center">
      <h1 className="display-5">404</h1>
      <p className="lead">This page does not exist.</p>
      <Link to="/dashboard" className="btn btn-primary">
        Go to the dashboard
      </Link>
    </div>
  );
}