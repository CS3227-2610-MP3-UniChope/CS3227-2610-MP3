import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import App from '../App';
import { apiFixture, deferred, failure, json, student, type Actor } from './api-fixture';

beforeEach(() => { window.history.replaceState({}, '', '/login'); });
afterEach(() => { vi.unstubAllGlobals(); });
async function fillLogin() {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText(/^email$/i), 'synthetic@example.test');
  await user.type(screen.getByLabelText(/^password$/i), 'Synthetic-password-12');
  return user;
}

test('FE1-LOGIN exact identity-free login, dynamic CSRF header and rotation before logout', async () => {
  const api = apiFixture();
  render(<App />);
  const user = await fillLogin();
  expect(screen.queryByRole('combobox', { name: /role/i })).not.toBeInTheDocument();
  expect(screen.getByLabelText(/^password$/i)).toHaveAttribute('autocomplete', 'current-password');
  await user.click(screen.getByRole('button', { name: /^sign in$/i }));
  await screen.findByRole('button', { name: /sign out/i });
  const login = api.requests('/api/auth/login', 'POST');
  expect(login).toHaveLength(1);
  expect(login[0].body).toEqual({ email: 'synthetic@example.test', password: 'Synthetic-password-12' });
  expect(login[0].headers.get('X-TEST-CSRF')).toBe('csrf-1');
  await waitFor(() => expect(api.requests('/api/auth/csrf').length).toBeGreaterThanOrEqual(2));
  expect(api.calls.indexOf(api.requests('/api/auth/csrf')[0])).toBeLessThan(api.calls.indexOf(login[0]));
  await user.click(screen.getByRole('button', { name: /sign out/i }));
  await screen.findByRole('button', { name: /^sign in$/i });
  const logout = api.requests('/api/auth/logout', 'POST');
  expect(logout).toHaveLength(1);
  expect(logout[0].body).toEqual({});
  expect(logout[0].headers.get('X-TEST-CSRF')).not.toBe('csrf-1');
  await waitFor(() => expect(api.requests('/api/auth/csrf').length).toBeGreaterThanOrEqual(3));
  expect(api.calls.every(call => call.init.cache === 'no-store')).toBe(true);
  expect(api.calls.every(call => !call.init.credentials || call.init.credentials === 'same-origin')).toBe(true);
});

test.each(['STUDENT', 'TUTOR', 'ADMIN'] as const)('FE1-RESTORE restored %s resolves wrong-role route to own home', async role => {
  const actor: Actor = { ...student, role };
  const api = apiFixture(actor);
  window.history.replaceState({}, '', role === 'STUDENT' ? '/admin' : '/student/slots');
  render(<App />);
  await screen.findByRole('button', { name: /sign out/i });
  const target = role === 'STUDENT' ? '/student/slots' : `/${role.toLowerCase()}`;
  await waitFor(() => expect(window.location.pathname).toBe(target));
  expect(api.requests('/api/auth/me')).toHaveLength(1);
  if (role !== 'STUDENT') {
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(new RegExp(role, 'i'));
    expect(screen.getByText(/workspace is not ready yet/i)).toBeVisible();
    expect(api.calls.some(call => call.url.pathname.startsWith('/api/student'))).toBe(false);
    expect(screen.getAllByRole('button')).toHaveLength(1);
  }
});

test.each(['/', '/login', '/student/bookings', '/tutor', '/admin'])('FE1-RESTORE anonymous route %s shows Login without protected data', async path => {
  const api = apiFixture();
  window.history.replaceState({}, '', path);
  render(<App />);
  await screen.findByLabelText(/^email$/i);
  expect(window.location.pathname).toBe('/login');
  expect(api.calls.some(call => call.url.pathname.startsWith('/api/student'))).toBe(false);
});

test('FE1-RESTORE no workspace flash while me is unresolved; authenticated Login redirects', async () => {
  const api = apiFixture(student);
  const pending = deferred<Response>();
  api.on('GET', '/api/auth/me', () => pending.promise);
  render(<App />);
  expect(screen.queryByRole('button', { name: /sign out/i })).not.toBeInTheDocument();
  expect(api.calls.some(call => call.url.pathname.startsWith('/api/student'))).toBe(false);
  await act(async () => pending.resolve(json({ ...student, role: 'ADMIN' })));
  await screen.findByRole('heading', { level: 1, name: /admin/i });
  expect(window.location.pathname).toBe('/admin');
});

test('FE1-RESTORE unknown route has accessible not-found and home action', async () => {
  apiFixture(student);
  window.history.replaceState({}, '', '/not-a-real-page');
  render(<App />);
  await screen.findByRole('heading', { level: 1, name: /not found/i });
  expect(screen.getByRole('link', { name: /home/i })).toBeVisible();
});

test.each([
  [401, 'INVALID_CREDENTIALS', /email|password|credentials|sign in/i],
  [429, 'AUTH_RATE_LIMITED', /wait|later|retry|try again/i],
  [503, 'TEMPORARILY_UNAVAILABLE', /try again|retry|unavailable/i],
] as const)('FE1-LOGIN safe %s feedback clears password without automatic replay', async (status, code, message) => {
  const api = apiFixture();
  api.on('POST', '/api/auth/login', () => failure(status, code, { 'Retry-After': '60' }));
  render(<App />);
  const user = await fillLogin();
  await user.click(screen.getByRole('button', { name: /^sign in$/i }));
  expect(await screen.findByRole('alert')).toHaveTextContent(message);
  expect(screen.getByLabelText(/^password$/i)).toHaveValue('');
  expect(api.requests('/api/auth/login', 'POST')).toHaveLength(1);
});

test.each(['network', 'malformed'] as const)('FE1-LOGIN %s failure is safe and leaves usable Login', async kind => {
  const api = apiFixture();
  api.on('POST', '/api/auth/login', () => {
    if (kind === 'network') throw new TypeError('private upstream diagnostic');
    return new Response('<html>private upstream diagnostic</html>', { status: 200, headers: { 'Content-Type': 'text/html' } });
  });
  render(<App />);
  const user = await fillLogin();
  await user.click(screen.getByRole('button', { name: /^sign in$/i }));
  await screen.findByRole('alert');
  expect(screen.queryByText(/private upstream diagnostic/)).not.toBeInTheDocument();
  expect(screen.getByLabelText(/^password$/i)).toHaveValue('');
  expect(api.requests('/api/auth/login', 'POST')).toHaveLength(1);
});

test('FE1-LOGIN failed CSRF prevents credentials POST and permits explicit retry', async () => {
  const api = apiFixture();
  api.on('GET', '/api/auth/csrf', () => failure(503, 'TEMPORARILY_UNAVAILABLE'));
  render(<App />);
  await screen.findByLabelText(/^email$/i);
  const user = userEvent.setup();
  const signIn = screen.getByRole('button', { name: /^sign in$/i });
  if (!signIn.hasAttribute('disabled')) {
    await user.type(screen.getByLabelText(/^email$/i), 'synthetic@example.test');
    await user.type(screen.getByLabelText(/^password$/i), 'Synthetic-password-12');
    await user.click(signIn);
  }
  await screen.findByRole('alert');
  expect(api.requests('/api/auth/login', 'POST')).toHaveLength(0);
  expect(screen.getByRole('button', { name: /retry|try again/i })).toBeVisible();
});

test('FE1-LOGOUT network failure clears identity immediately and offers server sign-out retry', async () => {
  const api = apiFixture({ ...student, role: 'TUTOR' });
  api.on('POST', '/api/auth/logout', () => { throw new TypeError('lost response'); });
  window.history.replaceState({}, '', '/tutor');
  render(<App />);
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /sign out/i }));
  await screen.findByLabelText(/^email$/i);
  expect(screen.queryByText(student.name)).not.toBeInTheDocument();
  expect(await screen.findByRole('alert')).toHaveTextContent(/sign.out|confirm|server/i);
  expect(screen.getByRole('button', { name: /retry.*sign.out|sign.out.*again/i })).toBeVisible();
  expect(api.requests('/api/auth/me')).toHaveLength(1);
  expect(api.requests('/api/auth/logout', 'POST')).toHaveLength(1);
});

test('FE1-PRIVACY no private browser-storage writes and untrusted actor names remain text', async () => {
  const local = vi.spyOn(Storage.prototype, 'setItem');
  const name = '<img src=x onerror="window.syntheticXss=true">';
  apiFixture({ ...student, name });
  window.history.replaceState({}, '', '/admin');
  render(<App />);
  await screen.findByText(name);
  expect(document.querySelector('img')).toBeNull();
  expect(local).not.toHaveBeenCalled();
});

test('FE1-UI Login is keyboard reachable, labelled and has one main heading plus skip link', async () => {
  apiFixture();
  render(<App />);
  await screen.findByLabelText(/^email$/i);
  expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
  expect(screen.getByRole('main')).toBeVisible();
  const skip = screen.getByRole('link', { name: /skip/i });
  skip.focus();
  expect(skip).toHaveFocus();
  screen.getByLabelText(/^email$/i).focus();
  const user = userEvent.setup();
  await user.tab();
  expect(screen.getByLabelText(/^password$/i)).toHaveFocus();
});

test('FE1-LOGOUT clears identity immediately and prevents Login racing an unresolved server logout', async () => {
  const api = apiFixture({ ...student, role: 'TUTOR' });
  const logout = deferred<Response>();
  api.on('POST', '/api/auth/logout', () => logout.promise);
  window.history.replaceState({}, '', '/tutor');
  render(<App />);
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /sign out/i }));
  await screen.findByLabelText(/^email$/i);
  expect(screen.queryByText(student.name)).not.toBeInTheDocument();
  expect(screen.queryByRole('heading', { level: 1, name: /tutor/i })).not.toBeInTheDocument();
  const signIn = screen.getByRole('button', { name: /^sign in$/i });
  expect(signIn).toBeDisabled();
  await user.click(signIn);
  expect(api.requests('/api/auth/login', 'POST')).toHaveLength(0);
  await waitFor(() => expect(api.requests('/api/auth/logout', 'POST')).toHaveLength(1));
  await act(async () => logout.resolve(new Response(null, { status: 204 })));
  await waitFor(() => expect(signIn).toBeEnabled());
  expect(screen.getByLabelText(/^email$/i)).toBeEnabled();
  expect(api.requests('/api/auth/login', 'POST')).toHaveLength(0);
  expect(api.requests('/api/auth/me')).toHaveLength(1);
  expect(api.requests('/api/auth/csrf').length).toBeGreaterThanOrEqual(2);
});
