import { useState } from 'react';
import { ApiError } from './api';
import { Brand, Icon, Link } from './components';
import { ThemeSwitch } from './theme';
export default function Login({ login, message, retryLogout, signingOut = false }: { login: (email: string, password: string) => Promise<void>; message: string; retryLogout?: () => void; signingOut?: boolean }) {
  const [email, setEmail] = useState(''); const [password, setPassword] = useState('');
  const [pending, setPending] = useState(false); const [error, setError] = useState(''); const [csrfFailed, setCsrfFailed] = useState(false);
  async function submit() {
    if (pending || signingOut) return;
    setPending(true); setError(''); setCsrfFailed(false);
    try { await login(email, password); } catch (e) {
      const failure = e instanceof ApiError ? e : new ApiError();
      setError((failure.status === 401 && failure.code !== 'INVALID_CREDENTIALS') || (failure.status === 403 && ['ACCOUNT_INACTIVE', 'CSRF_INVALID'].includes(failure.code)) ? 'Please try again.' : failure.code === 'INVALID_CREDENTIALS' ? 'The email or password is incorrect. Please try again.' : failure.code === 'AUTH_RATE_LIMITED' ? `Too many sign-in attempts. Please wait${/^\d+$/.test(failure.retryAfter) ? ` ${Math.min(Number(failure.retryAfter), 86400)} seconds` : ' a little'} before trying again.` : 'Sign in is temporarily unavailable. Please try again.');
      setCsrfFailed(true);
    } finally { setPassword(''); setPending(false); }
  }
  return <div className="login-page"><header className="login-header"><Brand /><span className="quiet">Your time, well spent.</span><ThemeSwitch /></header>
    <main id="main-content" tabIndex={-1} className="login-main"><div className="login-intro"><span className="eyebrow">A LITTLE TIME. A LOT OF CLARITY.</span><p className="login-intro-title">Make room for<br />your next question.</p><p>Find a consultation, choose a time, and keep your learning moving.</p><div className="login-note"><Icon name="calendar" /><span>A simpler way to connect<br /><strong>One consultation at a time.</strong></span></div></div>
      <section className="login-form-panel" aria-labelledby="login-title"><span className="eyebrow">WELCOME TO UNICHOPE</span><h1 id="login-title">Sign in</h1><p className="muted">Your next conversation starts here.</p>
        {(error || message) && <div role="alert" className="notice error">{error || message}</div>}
        {retryLogout && <button className="btn btn-outline-secondary mb-3" onClick={retryLogout}>Retry sign out</button>}
        {signingOut && <p role="status">Signing out of your session…</p>}
        <form onSubmit={event => { event.preventDefault(); void submit(); }}><label htmlFor="email">Email</label><input className="form-control" id="email" type="email" autoComplete="username" required value={email} onChange={event => setEmail(event.target.value)} disabled={pending || signingOut} />
          <label htmlFor="password">Password</label><input className="form-control" id="password" type="password" autoComplete="current-password" required value={password} onChange={event => setPassword(event.target.value)} disabled={pending || signingOut} />
          <button className="btn btn-primary login-submit" disabled={pending || signingOut} type="submit">{pending ? 'Signing in…' : 'Sign in'}<Icon name="arrow" /></button>
        </form>{csrfFailed && <button className="text-button mt-3" onClick={() => { setError(''); setCsrfFailed(false); document.getElementById('password')?.focus(); }}>Try again</button>}
        <p className="login-help">Use your UniChope account to continue.</p>{!pending && !signingOut && <p><Link href="/register">Create a Student account</Link></p>}
      </section></main><footer className="login-footer">A dedicated space for better conversations.</footer></div>;
}

