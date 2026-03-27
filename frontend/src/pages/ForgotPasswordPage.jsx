import { useState } from 'react';
import { Link } from 'react-router-dom';
import { requestPasswordReset } from '../api/authApi';
import Notice from '../components/Notice';
import { extractApiError } from '../utils/errors';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [response, setResponse] = useState(null);
  const [error, setError] = useState('');

  async function onSubmit(event) {
    event.preventDefault();
    setError('');
    setResponse(null);

    try {
      const result = await requestPasswordReset(email);
      setResponse(result);
    } catch (requestError) {
      setError(extractApiError(requestError, 'Could not request reset token.'));
    }
  }

  return (
    <section className="auth-container">
      <div className="hero-text">
        <h1>Password reset</h1>
        <p>Request a temporary reset token and confirm it on the next screen.</p>
      </div>

      <form className="auth-card glass-card" onSubmit={onSubmit}>
        <h2>Reset Request</h2>
        <label>
          Email
          <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
        </label>

        <Notice type="error" text={error} />
        {response ? (
          <Notice
            type="success"
            text={`${response.message} Token: ${response.resetToken} (expires in ${response.expiresInSeconds}s)`}
          />
        ) : null}

        <button type="submit">Request token</button>
        <p className="auth-links"><Link to="/reset-password">I already have a token</Link></p>
      </form>
    </section>
  );
}

