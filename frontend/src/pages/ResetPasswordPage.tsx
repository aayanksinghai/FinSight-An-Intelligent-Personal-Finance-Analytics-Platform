import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { confirmPasswordReset, getPasswordPolicy } from '../api/authApi';
import Notice from '../components/Notice';
import { extractApiError } from '../utils/errors';
import {
  DEFAULT_PASSWORD_POLICY,
  getPasswordPolicyHint,
  isStrongPassword,
} from '../utils/validation';
import type { PasswordPolicyResponse } from '../types/api';

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [passwordPolicy, setPasswordPolicy] = useState<PasswordPolicyResponse>(DEFAULT_PASSWORD_POLICY);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getPasswordPolicy()
      .then(setPasswordPolicy)
      .catch(() => { /* keep default */ });
  }, []);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);

    if (!isStrongPassword(newPassword, passwordPolicy)) {
      setError(getPasswordPolicyHint(passwordPolicy));
      setLoading(false);
      return;
    }

    try {
      await confirmPasswordReset(resetToken, newPassword);
      setMessage('Password reset successful. Redirecting to login...');
      setTimeout(() => navigate('/login'), 1200);
    } catch (err) {
      setError(extractApiError(err, 'Reset confirmation failed.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">
      <div className="w-full max-w-md animate-slide-up">
        <div className="mb-6 text-center">
          <h1 className="text-3xl font-extrabold text-[#edf2ff]">Reset your password</h1>
          <p className="mt-2 text-sm text-muted">
            Paste your reset token and choose a strong new password.
          </p>
        </div>

        <form
          className="glass-card p-7"
          onSubmit={(e) => void onSubmit(e)}
          id="reset-password-form"
        >
          <div className="flex flex-col gap-4">
            <div className="field">
              <label htmlFor="reset-token" className="form-label">Reset token</label>
              <input
                id="reset-token"
                type="text"
                className="form-input font-mono text-xs"
                value={resetToken}
                onChange={(e) => setResetToken(e.target.value)}
                required
                placeholder="Paste your token here"
              />
            </div>

            <div className="field">
              <label htmlFor="new-password" className="form-label">New password</label>
              <input
                id="new-password"
                type="password"
                className="form-input"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                autoComplete="new-password"
                placeholder="••••••••"
              />
              <p className="mt-1.5 text-[11px] text-muted">{getPasswordPolicyHint(passwordPolicy)}</p>
            </div>

            {message && <Notice type="success" text={message} />}
            {error && <Notice type="error" text={error} />}

            <button
              id="reset-submit"
              type="submit"
              className="btn-primary w-full"
              disabled={loading}
            >
              {loading ? <><span className="spinner" /> Updating...</> : 'Confirm reset'}
            </button>
          </div>

          <p className="mt-5 text-center text-sm">
            <Link to="/login" className="text-muted hover:text-brand">← Back to sign in</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
