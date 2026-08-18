import { useState } from 'react';

import * as authApi from '../api/authApi';
import { useAuth } from '../auth/AuthContext';
import { useRouter } from '../utils/router';

export function validatePassword(password) {
  if (password.length < 8) return 'Password must be at least 8 characters long';
  if (!/[a-zA-Z]/.test(password)) return 'Password must contain at least one letter';
  if (!/[0-9]/.test(password)) return 'Password must contain at least one number';
  if (!/[^a-zA-Z0-9]/.test(password)) return 'Password must contain at least one special character';
  return null;
}

export default function AuthPage() {
  const [mode, setMode] = useState('login');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
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
    <main className="auth-layout">
      <section className="auth-intro" aria-labelledby="intro-title">
        <div className="brand brand-inverse">
          <span className="brand-mark brand-mark-light" aria-hidden="true">ET</span>
          <span><strong>Event Ticketing</strong></span>
        </div>
        <div className="auth-intro-copy">
          <p className="eyebrow">Flexible venues. Clear operations.</p>
          <h1 id="intro-title">Manage every performance from one simple workspace.</h1>
          <p>
            Keep events and venues organised today, with floor plans, sessions and ticketing ready
            to grow into the next MVP iterations.
          </p>
        </div>
        <p className="auth-intro-footnote">Sprint 1 · Event Ticketing MVP</p>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <p className="eyebrow">{isRegistering ? 'Create your account' : 'Welcome back'}</p>
          <h2>{isRegistering ? 'Start managing events' : 'Log in to your workspace'}</h2>
          <p className="auth-subtitle">
            {isRegistering
              ? 'Use your details to create an MVP account.'
              : 'Enter the credentials used during registration.'}
          </p>

          <form onSubmit={handleSubmit} noValidate>
            {isRegistering && (
              <div className="field">
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

            <div className="field">
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

            <div className="field">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                name="password"
                type="password"
                autoComplete={isRegistering ? 'new-password' : 'current-password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Enter your password"
                required
              />
              {isRegistering && (
                <small>At least 8 characters with a letter, number and special character.</small>
              )}
            </div>

            {message && <div className="form-message form-message-error" role="alert">{message}</div>}

            <button className="button button-primary button-full" type="submit" disabled={submitting}>
              {submitting ? 'Please wait…' : isRegistering ? 'Create account' : 'Log in'}
            </button>
          </form>

          <p className="auth-toggle">
            {isRegistering ? 'Already have an account?' : "Don't have an account?"}
            {' '}
            <button type="button" onClick={switchMode}>
              {isRegistering ? 'Return to login' : 'Create an account'}
            </button>
          </p>
        </div>
      </section>
    </main>
  );
}
