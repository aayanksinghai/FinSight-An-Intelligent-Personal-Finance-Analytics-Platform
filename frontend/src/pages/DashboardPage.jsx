import { useAuthStore } from '../store/authStore';

export default function DashboardPage() {
  const { userEmail, role } = useAuthStore();

  return (
    <section className="panel-grid">
      <article className="panel glass-card">
        <h2>Welcome back</h2>
        <p className="muted">Manage auth, profile, and role-based admin actions from one place.</p>
        <div className="kpi-row">
          <div className="kpi-card">
            <span className="kpi-label">Signed in as</span>
            <strong>{userEmail || 'Unknown user'}</strong>
          </div>
          <div className="kpi-card">
            <span className="kpi-label">Role</span>
            <strong>{role || 'USER'}</strong>
          </div>
        </div>
      </article>

      <article className="panel glass-card">
        <h3>Current backend features</h3>
        <ul className="feature-list">
          <li>Register, login, refresh, logout, logout-all</li>
          <li>Password reset request and confirm</li>
          <li>Profile read/update/delete and security overview</li>
          <li>Admin user list + deactivate/reactivate (role protected)</li>
        </ul>
      </article>
    </section>
  );
}

