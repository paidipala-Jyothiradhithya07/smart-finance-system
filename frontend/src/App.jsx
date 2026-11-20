import React from 'react';
import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import NotFound from './pages/NotFound';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route element={<Layout />}> {/* layout with navbar, applies to protected routes */}
        <Route element={<ProtectedRoute><Dashboard /></ProtectedRoute>} path="/dashboard" />
        {/* Add other protected routes later */}
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
