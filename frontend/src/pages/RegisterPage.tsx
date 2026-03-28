import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getPasswordPolicy, login, register } from '../api/authApi';
import Notice from '../components/Notice';
import { extractApiError } from '../utils/errors';
import { useAuthStore } from '../store/authStore';
import {
  DEFAULT_PASSWORD_POLICY,
  getPasswordPolicyHint,
  isStrongPassword,
} from '../utils/validation';
import type { PasswordPolicyResponse } from '../types/api';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { setSession } = useAuthStore();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordPolicy, setPasswordPolicy] = useState<PasswordPolicyResponse>(DEFAULT_PASSWORD_POLICY);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getPasswordPolicy()
      .then(setPasswordPolicy)
      .catch(() => { /* keep default */ });
  }, []);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setLoading(true);
    setError('');
    setMessage('');

    if (!isStrongPassword(password, passwordPolicy)) {
      setError(getPasswordPolicyHint(passwordPolicy));
      setLoading(false);
      return;
    }

    try {
      await register(email, password);
      try {
        const tokenResponse = await login(email, password);
        setSession(tokenResponse.accessToken, tokenResponse.refreshToken);
        setMessage('Registration successful. Redirecting...');
        setTimeout(() => navigate('/'), 500);
      } catch {
        setMessage('Registration successful. Please sign in.');
        setTimeout(() => navigate('/login'), 700);
      }
    } catch (err) {
      setError(extractApiError(err, 'Registration failed.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="grid min-h-screen grid-cols-1 md:grid-cols-[1.2fr_1fr] items-center gap-6 px-6 py-10 md:px-12">
      {/* ── Hero ── */}
      <div className="animate-slide-up">
        <h1 className="mb-4 text-4xl font-extrabold tracking-tight text-[#edf2ff] md:text-5xl">
          Start your<br />
          <span className="bg-gradient-to-r from-brand to-brand-purple bg-clip-text text-transparent">
            financial journey
          </span>
        </h1>
        <p className="max-w-md text-base leading-relaxed text-muted">
          Upload your bank statements. Get ML-powered insights on your spending,
          anomalies, forecasts, and financial health — all in one intelligent dashboard.
        </p>

        <div className="mt-8 flex flex-col gap-3">
          {[
            '✅  Personalized anomaly detection',
            '✅  Category spend forecasting',
            '✅  Financial stress score (0–100)',
            '✅  What-if scenario simulator',
            '✅  AI chat assistant',
          ].map((feat) => (
            <p key={feat} className="text-sm text-muted">{feat}</p>
          ))}
        </div>
      </div>

      {/* ── Form card ── */}
      <div className="animate-slide-up">
        <form
          className="glass-card w-full max-w-md mx-auto p-7"
          onSubmit={(e) => void onSubmit(e)}
          id="register-form"
        >
          <h2 className="mb-1 text-xl font-bold text-[#edf2ff]">Create account</h2>
          <p className="mb-6 text-sm text-muted">Get started in seconds</p>

          <div className="flex flex-col gap-4">
            <div className="field">
              <label htmlFor="reg-email" className="form-label">Email</label>
              <input
                id="reg-email"
                type="email"
                className="form-input"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                placeholder="you@example.com"
              />
            </div>

            <div className="field">
              <label htmlFor="reg-password" className="form-label">Password</label>
              <input
                id="reg-password"
                type="password"
                className="form-input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="new-password"
                placeholder="••••••••"
              />
              <p className="mt-1.5 text-[11px] text-muted">{getPasswordPolicyHint(passwordPolicy)}</p>
            </div>

            {message && <Notice type="success" text={message} />}
            {error && <Notice type="error" text={error} />}

            <button
              id="register-submit"
              type="submit"
              className="btn-primary w-full mt-1"
              disabled={loading}
            >
              {loading ? (
                <><span className="spinner" /> Creating account...</>
              ) : (
                'Create account'
              )}
            </button>
          </div>

          <p className="mt-5 text-center text-sm text-muted">
            Already registered?{' '}
            <Link to="/login" className="text-brand hover:underline">Sign in</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
