import type { FormEvent } from 'react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { requestPasswordReset } from '../api/authApi';
import Notice from '../components/Notice';
import { extractApiError } from '../utils/errors';
import type { PasswordResetResponse } from '../types/api';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [response, setResponse] = useState<PasswordResetResponse | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError('');
    setResponse(null);
    setLoading(true);

    try {
      const result = await requestPasswordReset(email);
      setResponse(result);
    } catch (err) {
      setError(extractApiError(err, 'Could not request reset token.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">
      <div className="w-full max-w-md animate-slide-up">
        <div className="mb-6 text-center">
          <h1 className="text-3xl font-extrabold text-[#edf2ff]">Forgot password?</h1>
          <p className="mt-2 text-sm text-muted">
            Enter your email to receive a temporary reset token.
          </p>
        </div>

        <form
          className="glass-card p-7"
          onSubmit={(e) => void onSubmit(e)}
          id="forgot-password-form"
        >
          <div className="flex flex-col gap-4">
            <div className="field">
              <label htmlFor="reset-email" className="form-label">Email address</label>
              <input
                id="reset-email"
                type="email"
                className="form-input"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="you@example.com"
              />
            </div>

            {error && <Notice type="error" text={error} />}

            {response && (
              <Notice type="success">
                <span className="font-medium">{response.message}</span>
                <br />
                <span className="mt-1 block font-mono text-xs break-all">
                  Token: {response.resetToken}
                </span>
                <span className="mt-0.5 block text-xs opacity-75">
                  Expires in {response.expiresInSeconds}s
                </span>
              </Notice>
            )}

            <button
              id="forgot-submit"
              type="submit"
              className="btn-primary w-full"
              disabled={loading}
            >
              {loading ? <><span className="spinner" /> Sending...</> : 'Request reset token'}
            </button>
          </div>

          <div className="mt-5 flex justify-between text-sm">
            <Link to="/login" className="text-muted hover:text-brand">← Back to login</Link>
            <Link to="/reset-password" className="text-brand hover:underline">
              I have a token
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
