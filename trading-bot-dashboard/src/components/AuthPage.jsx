import { useState } from 'react';
import { login, register } from '../services/api.js';

export default function AuthPage({ onAuthenticated }) {
  const [mode, setMode] = useState('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const registering = mode === 'register';

  async function submit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    try {
      const account = registering
        ? await register({ name, email, password })
        : await login({ email, password });
      onAuthenticated(account);
    } catch (requestError) {
      setError(requestError.message || 'Unable to sign in. Please try again.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-screen">
      <section className="auth-showcase">
        <a className="brand auth-brand" href="/" aria-label="Quantflow home">
          <span className="brand-mark">↗</span><span>quant<span className="accent">flow</span></span>
        </a>
        <div className="showcase-copy">
          <span className="auth-kicker"><span className="showcase-live-dot" /> MARKET INTELLIGENCE, IN FOCUS</span>
          <h1>Make every move<br /><span>with clarity.</span></h1>
          <p>A calm, focused workspace for your portfolio, market signals, and trading activity.</p>
        </div>
        <div className="showcase-chart" aria-hidden="true">
          <div className="showcase-chart-top"><span>PORTFOLIO PERFORMANCE</span><strong>Live market insights</strong></div>
          <svg viewBox="0 0 520 160" role="presentation">
            <defs><linearGradient id="auth-fill" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stopColor="#79e9bb" stopOpacity=".3" /><stop offset="1" stopColor="#79e9bb" stopOpacity="0" /></linearGradient></defs>
            <path d="M0 125 C35 115 45 120 70 98 S110 106 135 82 S175 105 205 70 S245 80 270 55 S315 76 344 46 S390 68 416 30 S463 55 490 18 L520 28 L520 160 L0 160Z" fill="url(#auth-fill)" />
            <path d="M0 125 C35 115 45 120 70 98 S110 106 135 82 S175 105 205 70 S245 80 270 55 S315 76 344 46 S390 68 416 30 S463 55 490 18 L520 28" fill="none" stroke="#79e9bb" strokeWidth="3" />
          </svg>
          <div className="showcase-chart-foot"><span>Secure account access</span><span>● Protected by role-based access</span></div>
        </div>
        <div className="showcase-foot">BUILT FOR A MORE CONFIDENT INVESTING EXPERIENCE</div>
      </section>
      <section className="auth-entry">
        <div className="auth-form-wrap">
          <div className="auth-mobile-brand"><span className="brand-mark">↗</span> quant<span className="accent">flow</span></div>
          <span className="eyebrow">{registering ? 'GET STARTED' : 'WELCOME BACK'}</span>
          <h2>{registering ? 'Create your account' : 'Sign in to Quantflow'}</h2>
          <p className="auth-subtitle">{registering ? 'Set up your secure trading workspace.' : 'Your portfolio dashboard is one step away.'}</p>
          <form className="auth-form" onSubmit={submit}>
            {registering && <label>Full name<input autoComplete="name" required maxLength={80} value={name} onChange={(event) => setName(event.target.value)} placeholder="Your name" /></label>}
            <label>Email address<input type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" /></label>
            <label>Password<input type="password" autoComplete={registering ? 'new-password' : 'current-password'} required minLength={registering ? 8 : undefined} value={password} onChange={(event) => setPassword(event.target.value)} placeholder={registering ? 'At least 8 characters' : 'Enter your password'} /></label>
            {error && <div className="auth-error" role="alert">{error}</div>}
            {registering && <p className="role-note">The first account is the workspace administrator. New accounts are created with the user role.</p>}
            <button className="auth-submit" type="submit" disabled={busy}>{busy ? 'Please wait…' : registering ? 'Create account' : 'Sign in'}<span aria-hidden="true">→</span></button>
          </form>
          <p className="auth-switch">
            {registering ? 'Already have an account?' : 'New to Quantflow?'}
            <button type="button" onClick={() => { setMode(registering ? 'login' : 'register'); setError(''); }}>
              {registering ? 'Sign in' : 'Create an account'}
            </button>
          </p>
          <span className="auth-secure">🔒 Your account and portfolio are protected.</span>
        </div>
      </section>
    </main>
  );
}
