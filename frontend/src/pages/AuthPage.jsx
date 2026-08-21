import { useState } from 'react';

import * as authApi from '../api/authApi';
import { useAuth } from '../auth/AuthContext';
import { useRouter } from '../utils/router';
import './AuthPage.css';

export function validatePassword(password) {
  if (password.length < 8) return 'Password must be at least 8 characters long';
  if (!/[a-zA-Z]/.test(password)) return 'Password must contain at least one letter';
  if (!/[0-9]/.test(password)) return 'Password must contain at least one number';
  if (!/[^a-zA-Z0-9]/.test(password)) return 'Password must contain at least one special character';
  return null;
}

function initialMode() {
  if (typeof window !== 'undefined' && window.location.search.includes('register')) {
    return 'register';
  }
  return 'login';
}

export default function AuthPage() {
  const [mode, setMode] = useState(initialMode);
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { signIn } = useAuth();
  const { navigate } = useRouter();
  const isRegistering = mode === 'register';

  function switchMode() {
    setMode(isRegistering ? 'login' : 'register');
    setMessage('');
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setMessage('');

    const canonicalEmail = email.trim().toLowerCase();
    if (!canonicalEmail) {
      setMessage('Email is required');
      return;
    }

    if (isRegistering) {
      if (!fullName.trim()) {
        setMessage('Full name is required');
        return;
      }
      const passwordError = validatePassword(password);
      if (passwordError) {
        setMessage(passwordError);
        return;
      }
    }

    setSubmitting(true);
    try {
      const user = isRegistering
        ? await authApi.register({ fullName: fullName.trim(), email: canonicalEmail, password })
        : await authApi.login({ email: canonicalEmail, password });

      signIn(user);
      navigate('/dashboard', { replace: true });
    } catch (error) {
      setMessage(error.message || 'Request failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="au-wrap">
      <div className="au-card">
        <a className="au-back" href="/" onClick={(e) => { e.preventDefault(); navigate('/'); }}>
          &#8592; Back to home
        </a>

        <div className="au-brand">
          <span className="au-brand-mark" aria-hidden="true">P</span>
          <span className="au-brand-name">Perch</span>
        </div>

        <p className="au-eyebrow">{isRegistering ? 'Create your account' : 'Welcome back'}</p>
        <h1 className="au-title">{isRegistering ? 'Start with Perch' : 'Log in to your workspace'}</h1>
        <p className="au-sub">
          {isRegistering
            ? 'Set up an account to browse events or start selling your own.'
            : 'Enter the details you registered with.'}
        </p>

        <form onSubmit={handleSubmit} noValidate>
          {isRegistering && (
            <div className="au-field">
              <label htmlFor="fullName">Full name</label>
              <input
                id="fullName"
                name="fullName"
                type="text"
                autoComplete="name"
                value={fullName}
                onChange={(event) => setFullName(event.target.value)}
                placeholder="Jane Smith"
              />
            </div>
          )}

          <div className="au-field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              required
            />
          </div>

          <div className="au-field">
            <label htmlFor="password">Password</label>
            <div className="au-pw">
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete={isRegistering ? 'new-password' : 'current-password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Enter your password"
                required
              />
              <button
                type="button"
                className="au-pw-toggle"
                onClick={() => setShowPassword((v) => !v)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
            {isRegistering && (
              <small className="au-hint">At least 8 characters with a letter, number and special character.</small>
            )}
          </div>

          {message && <div className="au-error" role="alert">{message}</div>}

          <button className="au-submit" type="submit" disabled={submitting}>
            {submitting ? 'Please wait…' : isRegistering ? 'Create account' : 'Log in'}
          </button>
        </form>

        <p className="au-toggle">
          {isRegistering ? 'Already have an account?' : "Don't have an account?"}
          {' '}
          <button type="button" onClick={switchMode}>
            {isRegistering ? 'Return to login' : 'Create an account'}
          </button>
        </p>
      </div>
    </main>
  );
}
