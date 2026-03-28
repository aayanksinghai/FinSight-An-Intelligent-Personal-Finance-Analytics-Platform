import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { confirmPasswordReset, getPasswordPolicy } from '../api/authApi';
import Notice from '../components/Notice';
import { extractApiError } from '../utils/errors';
import { DEFAULT_PASSWORD_POLICY, getPasswordPolicyHint, isStrongPassword } from '../utils/validation';

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ resetToken: '', newPassword: '' });
  const [passwordPolicy, setPasswordPolicy] = useState(DEFAULT_PASSWORD_POLICY);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
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
    setError('');
    setMessage('');
    setLoading(true);

    if (!isStrongPassword(form.newPassword, passwordPolicy)) {
      setError(getPasswordPolicyHint(passwordPolicy));
      setLoading(false);
      return;
    }

    try {
      await confirmPasswordReset(form.resetToken, form.newPassword);
      setMessage('Password reset successful. Redirecting to login...');
      setTimeout(() => navigate('/login'), 1200);
    } catch (requestError) {
      setError(extractApiError(requestError, 'Reset confirmation failed.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="auth-container">
      <div className="hero-text">
        <h1>Confirm reset</h1>
        <p>Paste your reset token and set a strong password.</p>
      </div>

      <form className="auth-card glass-card" onSubmit={onSubmit}>
        <h2>Reset Password</h2>
        <label>
          Reset Token
          <input
            type="text"
            value={form.resetToken}
            onChange={(event) => setForm((prev) => ({ ...prev, resetToken: event.target.value }))}
            required
          />
        </label>
        <label>
          New Password
          <input
            type="password"
            value={form.newPassword}
            onChange={(event) => setForm((prev) => ({ ...prev, newPassword: event.target.value }))}
            required
          />
          <small className="muted">{getPasswordPolicyHint(passwordPolicy)}</small>
        </label>

        <Notice type="success" text={message} />
        <Notice type="error" text={error} />

        <button type="submit" disabled={loading}>{loading ? 'Updating...' : 'Confirm reset'}</button>
        <p className="auth-links"><Link to="/login">Back to sign in</Link></p>
      </form>
    </section>
  );
}

