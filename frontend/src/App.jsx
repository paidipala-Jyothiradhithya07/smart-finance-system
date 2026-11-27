import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Transactions from './pages/Transactions';
import StatementUpload from './pages/StatementUpload';
import MonthlyAnalysis from './pages/MonthlyAnalysis';
import NotFound from './pages/NotFound';
import TransactionForm from './pages/TransactionForm';
import UploadStatement from './pages/UploadStatement';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/income" element={<Transactions fixedType="CREDIT" />} />
          <Route path="/expenses" element={<Transactions fixedType="DEBIT" />} />
          <Route path="/statements" element={<StatementUpload />} />
          <Route path="/analysis" element={<MonthlyAnalysis />} />
          <Route path="transactions/new" element={<TransactionForm key="new" />} />
          <Route path="transactions/:id/edit" element={<TransactionForm key="edit" />} />
          <Route path="/upload" element={<UploadStatement />} />
        </Route>
      </Route>
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
