import { useState, type FormEvent } from 'react';
import { ApiError, safeError, type ApiClient } from './api';

export default function ChangePassword({ api, onComplete }: { api: ApiClient; onComplete: () => void }) {
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (pending) return;
    if (password !== confirmation) { setError('The passwords do not match.'); return; }
    setPending(true); setError('');
    try { await api.request('/api/auth/change-password', { password, confirmation }); onComplete(); }
    catch (failure) { setError(failure instanceof ApiError && failure.code === 'VALIDATION_FAILED' ? 'Use a password between 8 and 128 characters.' : safeError(failure)); }
    finally { setPassword(''); setConfirmation(''); setPending(false); }
  }
  return <main id="main-content" className="restore-screen">
    <section className="login-form-panel" aria-labelledby="change-password-title">
      <span className="eyebrow">SECURE YOUR ACCOUNT</span>
      <h1 id="change-password-title">Set your password</h1>
      <p className="muted">Your administrator provided a temporary password. Choose a new password before opening your Tutor workspace.</p>
      {error && <div role="alert" className="notice error">{error}</div>}
      <form onSubmit={event => void submit(event)}>
        <label htmlFor="new-password">New password</label>
        <input className="form-control" id="new-password" type="password" autoComplete="new-password" minLength={8} maxLength={128} required value={password} onChange={event => setPassword(event.target.value)} disabled={pending} />
        <label htmlFor="confirm-password">Confirm new password</label>
        <input className="form-control" id="confirm-password" type="password" autoComplete="new-password" minLength={8} maxLength={128} required value={confirmation} onChange={event => setConfirmation(event.target.value)} disabled={pending} />
        <button className="btn btn-primary login-submit" disabled={pending} type="submit">{pending ? 'Saving…' : 'Change password'}</button>
      </form>
    </section>
  </main>;
}
