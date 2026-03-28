import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getPasswordPolicy, login, register } from '../api/authApi';
import Notice from '../components/Notice';
import { extractApiError } from '../utils/errors';
import { useAuthStore } from '../store/authStore';
import { DEFAULT_PASSWORD_POLICY, getPasswordPolicyHint, isStrongPassword } from '../utils/validation';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { setSession } = useAuthStore();
  const [form, setForm] = useState({ email: '', password: '' });
  const [passwordPolicy, setPasswordPolicy] = useState(DEFAULT_PASSWORD_POLICY);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getPasswordPolicy()
      .then(setPasswordPolicy)
      .catch(() => {
        // Keep default policy when backend policy endpoint is unavailable.
      });
  }, []);

  async function onSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError('');
    setMessage('');

    if (!isStrongPassword(form.password, passwordPolicy)) {
      setError(getPasswordPolicyHint(passwordPolicy));
      setLoading(false);
      return;
    }

    try {
      await register(form.email, form.password);
      try {
        const tokenResponse = await login(form.email, form.password);
        setSession(tokenResponse.accessToken, tokenResponse.refreshToken);
        setMessage('Registration successful. Redirecting to dashboard...');
        setTimeout(() => navigate('/'), 500);
      } catch {
        setMessage('Registration successful. Please sign in. Redirecting to login...');
        setTimeout(() => navigate('/login'), 700);
      }
    } catch (requestError) {
      setError(extractApiError(requestError, 'Registration failed.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="auth-container">
      <div className="hero-text">
        <h1>Create your account</h1>
        <p>Password must include uppercase, lowercase, number, and special character.</p>
      </div>

      <form className="auth-card glass-card" onSubmit={onSubmit}>
        <h2>Register</h2>
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
          <small className="muted">{getPasswordPolicyHint(passwordPolicy)}</small>
        </label>

        <Notice type="success" text={message} />
        <Notice type="error" text={error} />

        <button type="submit" disabled={loading}>{loading ? 'Creating...' : 'Create account'}</button>
        <p className="auth-links"><Link to="/login">Already registered? Sign in</Link></p>
      </form>
    </section>
  );
}

