import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, expect, test, vi } from 'vitest';
import App from '../App';
import { apiFixture, deferred, json, student } from './api-fixture';
afterEach(() => vi.unstubAllGlobals());

test('Registration uses exact public contract, stays anonymous and returns to sign in', async () => {
  window.history.replaceState({}, '', '/register');
  const api = apiFixture();
  api.on('POST', '/api/auth/register', () => json({ message: 'Registration completed' }, 201));
  render(<App />);
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText(/^name$/i), 'New Student');
  await user.type(screen.getByLabelText(/^email$/i), 'new@example.edu');
  await user.type(screen.getByLabelText(/^password$/i), 'New-password-12');
  await user.type(screen.getByLabelText(/^confirm password$/i), 'New-password-12');
  await user.click(screen.getByRole('button', { name: /create account/i }));
  await screen.findByRole('heading', { name: /^sign in$/i });
  expect(api.requests('/api/auth/register')[0].body).toEqual({ name: 'New Student', email: 'new@example.edu', password: 'New-password-12', confirmation: 'New-password-12' });
  expect(api.requests('/api/auth/login')).toHaveLength(0);
  expect(screen.getByText(/Registration completed/)).toBeVisible();
});

test.each(['STUDENT','TUTOR'] as const)('%s cannot open an Admin subpage', async role => {
  window.history.replaceState({}, '', '/admin/users');
  const api = apiFixture({ ...student, role });
  render(<App />);
  await waitFor(() => expect(window.location.pathname).toBe(role === 'STUDENT' ? '/student/slots' : '/tutor'));
  expect(api.calls.some(call => call.url.pathname.startsWith('/api/admin/'))).toBe(false);
});

test('Navigating away from a pending login ignores its completion', async () => {
  window.history.replaceState({}, '', '/login');
  const api = apiFixture(); const pending = deferred<Response>();
  api.on('POST', '/api/auth/login', () => pending.promise);
  render(<App />); const user = userEvent.setup();
  await user.type(await screen.findByLabelText(/^email$/i), 'new@example.edu');
  await user.type(screen.getByLabelText(/^password$/i), 'New-password-12');
  await user.click(screen.getByRole('button', { name: /^sign in$/i }));
  expect(screen.queryByRole('link', { name: /create a student/i })).not.toBeInTheDocument();
  await act(async () => { window.history.pushState({}, '', '/register'); window.dispatchEvent(new PopStateEvent('popstate')); });
  await act(async () => pending.resolve(json({ ...student, role: 'ADMIN' })));
  expect(window.location.pathname).toBe('/register');
  expect(screen.getByRole('heading', { name: /create student/i })).toBeVisible();
});
