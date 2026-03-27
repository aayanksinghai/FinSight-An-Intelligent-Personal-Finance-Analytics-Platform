import { NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { logout } from '../api/authApi';

export default function Layout({ children }) {
  const navigate = useNavigate();
  const { role, userEmail, clearSession } = useAuthStore();

  async function handleSignOut() {
    try {
      await logout();
    } catch {
      // Ignore API errors and clear local auth state.
    } finally {
      clearSession();
      navigate('/login');
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar glass-card">
        <h1>FinSight</h1>
        <p className="muted">User Service Portal</p>
        <nav>
          <NavLink to="/" end>Dashboard</NavLink>
          <NavLink to="/profile">Profile</NavLink>
          {role === 'ADMIN' ? <NavLink to="/admin">Admin</NavLink> : null}
        </nav>
        <div className="user-chip">
          <span>{userEmail}</span>
          <small>{role || 'USER'}</small>
        </div>
        <button className="danger-btn" onClick={handleSignOut}>Sign out</button>
      </aside>
      <main className="main-content">{children}</main>
    </div>
  );
}

