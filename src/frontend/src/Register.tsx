import { useEffect, useRef, useState } from 'react';
import { ApiError, PreflightError } from './api';
import { Brand, Icon } from './components';
import { ThemeSwitch } from './theme';

export default function Register({ register, onSuccess, onSignIn }: {
  register: (name: string, email: string, password: string, confirmation: string) => Promise<void>;
  onSuccess: () => void;
  onSignIn: () => void;
}) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const submitting = useRef(false);
  const mounted = useRef(true);
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);

  async function submit() {
    if (submitting.current) return;
    setError('');
    if (!name.trim() || Array.from(name.trim()).length > 100) {
      setError('Enter a name of up to 100 characters.'); return;
    }
    if (Array.from(password).length < 8 || Array.from(password).length > 128) {
      setError('Use a password between 8 and 128 characters.'); return;
    }
    if (password !== confirmation) { setError('The passwords do not match.'); return; }
    submitting.current = true; setPending(true);
    try {
      await register(name.trim(), email.trim(), password, confirmation);
      if (!mounted.current) return;
      setPassword(''); setConfirmation('');
      onSuccess();
    } catch (failure) {
      if (!mounted.current) return;
      const code = failure instanceof ApiError ? failure.code : '';
      setError(failure instanceof PreflightError ? 'Nothing was sent. Please try again.'
        : code === 'ACCOUNT_UNAVAILABLE' ? 'An account cannot be created with that email. Try signing in or use another email.'
        : code === 'VALIDATION_FAILED' ? 'Check your name, email and password, then try again.'
        : code === 'CSRF_INVALID' ? 'Your security token expired. Please try again.'
        : 'We could not confirm whether your account was created. Try signing in before submitting again.');
    } finally {
      setPassword(''); setConfirmation(''); submitting.current = false; setPending(false);
    }
  }

  return <div className="login-page"><header className="login-header"><Brand /><span className="quiet">Your time, well spent.</span><ThemeSwitch /></header>
    <main id="main-content" tabIndex={-1} className="login-main"><div className="login-intro"><span className="eyebrow">A LITTLE TIME. A LOT OF CLARITY.</span><p className="login-intro-title">Your questions<br />have a place here.</p><p>Create a Student account to find consultations and manage your bookings.</p><div className="login-note"><Icon name="calendar" /><span>A simpler way to connect<br /><strong>One consultation at a time.</strong></span></div></div>
      <section className="login-form-panel" aria-labelledby="register-title"><span className="eyebrow">WELCOME TO UNICHOPE</span><h1 id="register-title">Create Student account</h1><p className="muted">Already have an account? <button type="button" className="text-button" onClick={onSignIn} disabled={pending}>Sign in</button></p>
        {error && <div role="alert" className="notice error">{error}</div>}
        <form onSubmit={event => { event.preventDefault(); void submit(); }}>
          <label htmlFor="register-name">Name</label><input className="form-control" id="register-name" autoComplete="name" required value={name} onChange={event => setName(event.target.value)} disabled={pending} />
          <label htmlFor="register-email">Email</label><input className="form-control" id="register-email" type="email" autoComplete="username" required maxLength={254} value={email} onChange={event => setEmail(event.target.value)} disabled={pending} />
          <label htmlFor="register-password">Password</label><input className="form-control" id="register-password" type="password" autoComplete="new-password" required aria-describedby="password-help" value={password} onChange={event => setPassword(event.target.value)} disabled={pending} /><p id="password-help" className="muted">Use 8–128 characters. Spaces count as part of your password.</p>
          <label htmlFor="register-confirmation">Confirm password</label><input className="form-control" id="register-confirmation" type="password" autoComplete="new-password" required value={confirmation} onChange={event => setConfirmation(event.target.value)} disabled={pending} />
          <button className="btn btn-primary login-submit" type="submit" disabled={pending}>{pending ? 'Creating account…' : 'Create account'}<Icon name="arrow" /></button>
        </form><p className="login-help">After registration, sign in with your new account.</p>
      </section></main><footer className="login-footer">A dedicated space for better conversations.</footer></div>;
}
