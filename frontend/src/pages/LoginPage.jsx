import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../api/authApi';
import { useAuthStore } from '../store/authStore';
import Notice from '../components/Notice';
import { extractApiError } from '../utils/errors';

export default function LoginPage() {
  const navigate = useNavigate();
  const { setSession } = useAuthStore();
  const [form, setForm] = useState({ email: 'demo@finsight.local', password: 'Passw0rd!123' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function onSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await login(form.email, form.password);
      setSession(response.accessToken, response.refreshToken);
      navigate('/');
    } catch (requestError) {
      setError(extractApiError(requestError, 'Login failed. Check your credentials.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="auth-container">
      <div className="hero-text">
        <h1>Welcome to FinSight</h1>
        <p>Securely manage authentication, profile controls, and admin operations with a responsive UX.</p>
      </div>

      <form className="auth-card glass-card" onSubmit={onSubmit}>
        <h2>Sign in</h2>
        <label>
          Email
          <input
            type="email"
            value={form.email}
            onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
            required
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={form.password}
            onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
            required
          />
        </label>

        <Notice type="error" text={error} />
        <button type="submit" disabled={loading}>{loading ? 'Signing in...' : 'Sign in'}</button>

        <p className="auth-links">
          <Link to="/register">Create account</Link>
          <Link to="/forgot-password">Forgot password</Link>
        </p>
      </form>
    </section>
  );
}

