import type { FormEvent } from 'react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../api/authApi';
import { useAuthStore } from '../store/authStore';
import Notice from '../components/Notice';
import { extractApiError } from '../utils/errors';

export default function LoginPage() {
  const navigate = useNavigate();
  const { setSession } = useAuthStore();
  const [email, setEmail] = useState('demo@finsight.local');
  const [password, setPassword] = useState('Passw0rd!123');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await login(email, password);
      setSession(response.accessToken, response.refreshToken);
      navigate('/');
    } catch (err) {
      setError(extractApiError(err, 'Login failed. Check your credentials.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="grid min-h-screen grid-cols-1 md:grid-cols-[1.2fr_1fr] items-center gap-6 px-6 py-10 md:px-12">
      {/* ── Hero text ── */}
      <div className="animate-slide-up">
        <div className="mb-4 inline-flex items-center rounded-full border border-brand/30 bg-brand/10 px-3 py-1 text-xs font-semibold text-brand">
          🚀 Personal Finance Analytics Platform
        </div>
        <h1 className="mb-4 text-4xl font-extrabold leading-tight tracking-tight text-[#edf2ff] md:text-5xl">
          Take control of<br />
          <span className="bg-gradient-to-r from-brand to-brand-purple bg-clip-text text-transparent">
            your finances
          </span>
        </h1>
        <p className="max-w-md text-base leading-relaxed text-muted">
          ML-powered spending insights, anomaly detection, spend forecasting,
          financial stress scoring, a what-if simulator, and an AI chat assistant
          grounded in your real transaction data.
        </p>

        <div className="mt-8 grid grid-cols-3 gap-4">
          {[
            { label: 'ML Services', value: '7' },
            { label: 'Microservices', value: '8' },
            { label: 'Insights', value: '∞' },
          ].map((stat) => (
            <div key={stat.label} className="kpi-card text-center">
              <span className="block text-2xl font-bold text-brand">{stat.value}</span>
              <span className="text-xs text-muted">{stat.label}</span>
            </div>
          ))}
        </div>
      </div>

      {/* ── Auth card ── */}
      <div className="animate-slide-up">
        <form
          className="glass-card w-full max-w-md mx-auto p-7"
          onSubmit={(e) => void onSubmit(e)}
          id="login-form"
        >
          <h2 className="mb-1 text-xl font-bold text-[#edf2ff]">Sign in</h2>
          <p className="mb-6 text-sm text-muted">Welcome back to FinSight</p>

          <div className="flex flex-col gap-4">
            <div className="field">
              <label htmlFor="login-email" className="form-label">Email</label>
              <input
                id="login-email"
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
              <label htmlFor="login-password" className="form-label">Password</label>
              <input
                id="login-password"
                type="password"
                className="form-input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
                placeholder="••••••••"
              />
            </div>

            {error && <Notice type="error" text={error} />}

            <button
              id="login-submit"
              type="submit"
              className="btn-primary w-full mt-1"
              disabled={loading}
            >
              {loading ? (
                <><span className="spinner" /> Signing in...</>
              ) : (
                'Sign in'
              )}
            </button>
          </div>

          <div className="mt-5 flex items-center justify-between text-sm">
            <Link to="/register" className="text-brand hover:underline">
              Create account
            </Link>
            <Link to="/forgot-password" className="text-muted hover:text-brand">
              Forgot password?
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
