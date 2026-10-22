import { useEffect, useState } from 'react';
import { ApiClient, ApiError, isActor, StaleRequest, type Actor } from './api';
import { Brand, Icon, Link, Loading } from './components';
import Login from './Login';
import Register from './Register';
import AdminWorkspace from './admin/AdminWorkspace';
import { navigate, useLocation } from './navigation';
import StudentWorkspace from './student/StudentWorkspace';
import TutorWorkspace from './tutor/TutorWorkspace';
import { ThemeProvider, ThemeSwitch } from './theme';
const adminPaths = ['/admin', '/admin/overview', '/admin/users', '/admin/modules', '/admin/assignments', '/admin/bookings', '/admin/reports'];
const knownPaths = ['/', '/login', '/register', '/tutor', '/student/slots', '/student/bookings', ...adminPaths];
export const home = (actor: Actor) => actor.role === 'STUDENT' ? '/student/slots' : `/${actor.role.toLowerCase()}`;
export default function App() {
  return <ThemeProvider><SessionApp /></ThemeProvider>;
}
function SessionApp() {
  const [api] = useState(() => new ApiClient());
  const [actor, setActor] = useState<Actor | null>(null); const [restoring, setRestoring] = useState(true);
  const [message, setMessage] = useState(''); const [logoutFailed, setLogoutFailed] = useState(false);
  const [signingOut, setSigningOut] = useState(false);
  const [restoreFailed, setRestoreFailed] = useState(false); const [restoreAttempt, setRestoreAttempt] = useState(0); const location = useLocation();
  api.onSessionError = error => { api.reset(); setActor(null); setMessage(error.code === 'ACCOUNT_INACTIVE' ? 'Your account is inactive. Please sign in again or contact your administrator.' : 'Your session expired. Please sign in again.'); navigate('/login', true); };
  useEffect(() => {
    let active = true;
    setRestoring(true); setRestoreFailed(false); setMessage('');
    void api.request<unknown>('/api/auth/me', undefined, false).then(value => { if (!isActor(value)) throw new ApiError(); if (active) setActor(value); }).catch(error => {
      if (!active || error instanceof StaleRequest) return;
      if (error instanceof ApiError && (error.status === 401 || (error.status === 403 && error.code === 'ACCOUNT_INACTIVE'))) { if (error.code === 'ACCOUNT_INACTIVE') setMessage('Your account is inactive. Please sign in again.'); if (knownPaths.includes(window.location.pathname) && window.location.pathname !== '/register') navigate('/login', true); }
      else { setRestoreFailed(true); setMessage("We can't reach the server right now"); }
    }).finally(() => { if (active) setRestoring(false); });
    return () => { active = false; api.reset(); };
  }, [api, restoreAttempt]);
  useEffect(() => {
    if (restoring || restoreFailed) return;
    const path = window.location.pathname;
    if (!knownPaths.includes(path)) return;
    if (!actor) { if (!['/login', '/register'].includes(path)) navigate('/login', true); return; }
    const own = actor.role === 'STUDENT' ? path.startsWith('/student/') : actor.role === 'ADMIN' ? adminPaths.includes(path) : path === home(actor);
    if (!own) navigate(home(actor), true);
  }, [actor, restoring, restoreFailed, location]);
  async function login(email: string, password: string) {
    const loginPath = window.location.pathname;
    const value = await api.request<unknown>('/api/auth/login', { email, password }, false);
    if (!isActor(value)) throw new ApiError();
    if (window.location.pathname !== loginPath) { api.reset(); return; }
    api.reset();
    try { await api.csrf(); } catch (error) { if (error instanceof StaleRequest) return; }
    if (window.location.pathname !== loginPath) return;
    setLogoutFailed(false); setMessage(''); setActor(value); navigate(home(value), true);
  }
  async function register(name: string, email: string, password: string, confirmation: string) {
    await api.request('/api/auth/register', { name, email, password, confirmation }, false);
  }
  function registered() { setMessage('Registration completed. Sign in with your new account.'); navigate('/login', true); }
  async function logout() {
    if (signingOut) return; setSigningOut(true); setActor(null); setMessage(''); setLogoutFailed(false); api.reset(); navigate('/login', true);
    try {
      await api.request('/api/auth/logout', {}, false);
      api.reset();
      // The server has confirmed sign-out. Anonymous token refresh is best effort;
      // Login will acquire a fresh token on submit if this attempt fails.
      try { await api.csrf(); } catch { /* Keep the confirmed sign-out state. */ }
    }
    catch (error) { if (error instanceof ApiError && (error.status === 401 || (error.status === 403 && error.code === 'ACCOUNT_INACTIVE'))) { api.reset(); } else if (!(error instanceof StaleRequest)) { setLogoutFailed(true); setMessage('Server sign-out could not be confirmed. Retry sign out to close your session.'); } } finally { setSigningOut(false); }
  }
  const unknown = !knownPaths.includes(window.location.pathname);
  return <><a className="skip-link" href="#main-content">Skip to content</a>
    {(restoring || restoreFailed || unknown) && <header className="app-header"><Brand /><div className="account"><ThemeSwitch /></div></header>}
    {restoring ? <main id="main-content" className="restore-screen"><h1>Opening your workspace</h1><Loading label="Checking your session" /></main> : restoreFailed ? <main id="main-content" className="restore-screen"><h1>Unable to open your workspace</h1><p role="alert">{message}</p><button className="btn btn-primary" onClick={() => setRestoreAttempt(value => value + 1)}>Try again</button></main> : unknown ? <main id="main-content" className="restore-screen"><h1>Page not found</h1><p>This page could not be found.</p><Link href={actor ? home(actor) : '/login'}>Back to home</Link></main> : !actor ? window.location.pathname === '/register' ? <Register register={register} onSuccess={registered} onSignIn={() => navigate('/login')} /> : <Login login={login} message={message} signingOut={signingOut} retryLogout={logoutFailed ? () => void logout() : undefined} /> : <>
      <header className="app-header"><Link href={home(actor)} className="brand-link"><Brand /></Link>{actor.role === 'STUDENT' && <span className="header-caption">CONSULTATION PLANNER</span>}<div className="account">{actor.role !== 'TUTOR' && <span><strong>{actor.name}</strong><small>{actor.role.charAt(0) + actor.role.slice(1).toLowerCase()}</small></span>}<ThemeSwitch /><button className="btn sign-out" onClick={() => void logout()}><Icon name="logout" />Sign out</button></div></header>
      {actor.role === 'STUDENT' ? <StudentWorkspace key={actor.id} api={api} location={location} signOut={() => void logout()} /> : actor.role === 'TUTOR' ? <TutorWorkspace key={actor.id} api={api} actor={actor} /> : <AdminWorkspace key={actor.id} api={api} location={location} />}
    </>}
  </>;
}




