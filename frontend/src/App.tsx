import type { ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import DashboardPage from './pages/DashboardPage';
import ProfilePage from './pages/ProfilePage';
import AdminPage from './pages/AdminPage';
import UploadPage from './pages/UploadPage';
import BudgetPage from './pages/BudgetPage';
import TransactionsPage from './pages/TransactionsPage';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';

import { Toaster } from 'react-hot-toast';

function AppShell({ children }: { children: ReactNode }) {
  return (
    <ProtectedRoute>
      <Toaster position="top-right" toastOptions={{
        className: '!bg-brand-dark !text-white !border !border-white/10 !shadow-2xl',
        duration: 5000,
      }} />
      <Layout>{children}</Layout>
    </ProtectedRoute>
  );
}

export default function App() {
  return (
    <Routes>
      {/* Public auth routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />

      {/* Protected user routes */}
      <Route
        path="/"
        element={
          <AppShell>
            <DashboardPage />
          </AppShell>
        }
      />

      <Route
        path="/profile"
        element={
          <AppShell>
            <ProfilePage />
          </AppShell>
        }
      />

      <Route
        path="/upload"
        element={
          <AppShell>
            <UploadPage />
          </AppShell>
        }
      />

      <Route
        path="/budgets"
        element={
          <AppShell>
            <BudgetPage />
          </AppShell>
        }
      />

      <Route
        path="/transactions"
        element={
          <AppShell>
            <TransactionsPage />
          </AppShell>
        }
      />

      {/* Admin-only route */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute adminOnly>
            <Layout>
              <AdminPage />
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* Catch-all */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
