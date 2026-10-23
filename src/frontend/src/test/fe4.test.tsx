import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import App from '../App';
import { apiFixture, deferred, failure, ids, json, moduleOption, slot, student } from './api-fixture';

beforeEach(() => window.history.replaceState({}, '', '/student/slots?week=2026-10-19'));
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });
function bad(kind: string): Response {
  if (kind === 'network') throw new TypeError('Synthetic private diagnostic');
  if (kind === 'malformed') return json({ invalid: true });
  return failure(Number(kind), 'TEMPORARILY_UNAVAILABLE');
}
async function submit(user: ReturnType<typeof userEvent.setup>) {
  await user.type(await screen.findByLabelText(/^email$/i), 'synthetic@example.test');
  await user.type(screen.getByLabelText(/^password$/i), 'Synthetic-password-12');
  await user.click(screen.getByRole('button', { name: /^sign in$/i }));
}
test.each(['503', 'network', 'malformed', '500', '403'])('FE1-RESTORE-UNREACHABLE S1: %s keeps URL and safe retry screen', async kind => {
  const api = apiFixture();
  api.on('GET', '/api/auth/me', () => bad(kind));
  render(<App />);
  expect(await screen.findByRole('alert')).toHaveTextContent("We can't reach the server right now");
  expect(window.location.pathname + window.location.search).toBe('/student/slots?week=2026-10-19');
  expect(screen.queryByLabelText(/^email$/i)).not.toBeInTheDocument();
  expect(screen.queryByText(student.name)).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: /^try again$/i })).toBeEnabled();
  expect(api.requests('/api/auth/me')).toHaveLength(1);
});
for (const outcome of ['student', 'tutor', 'admin', '401', 'inactive', '503']) {
  test('FE1-RESTORE-UNREACHABLE S2 retry to ' + outcome, async () => {
    const api = apiFixture();
    api.on('GET', '/api/auth/me', () => bad('503'));
    render(<App />);
    const retry = await screen.findByRole('button', { name: /^try again$/i });
    const marker = document.documentElement;
    api.on('GET', '/api/auth/me', () => outcome === '401' ? failure(401, 'UNAUTHENTICATED') : outcome === 'inactive' ? failure(403, 'ACCOUNT_INACTIVE') : outcome === '503' ? bad('503') : json({ ...student, role: outcome.toUpperCase() }));
    await userEvent.setup().click(retry);
    await waitFor(() => expect(api.requests('/api/auth/me')).toHaveLength(2));
    expect(document.documentElement).toBe(marker);
    if (outcome === '503') expect(await screen.findByRole('alert')).toHaveTextContent("We can't reach the server right now");
    else if (outcome === '401' || outcome === 'inactive') {
      await screen.findByLabelText(/^email$/i);
      if (outcome === 'inactive') expect(screen.getByRole('alert')).toHaveTextContent(/inactive/i);
      else expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    } else await screen.findByRole('heading', { level: 1, name: outcome === 'student' ? /find a consultation/i : outcome === 'tutor' ? /manage consultations/i : /admin overview/i });
  });
}
test.each([[401, 'UNAUTHENTICATED'], [403, 'ACCOUNT_INACTIVE']] as const)('FE1-RESTORE-UNREACHABLE S3 initial %s', async (status, code) => {
  const api = apiFixture(); api.on('GET', '/api/auth/me', () => failure(status, code)); render(<App />);
  await screen.findByLabelText(/^email$/i);
  if (status === 401) expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  else expect(screen.getByRole('alert')).toHaveTextContent(/inactive/i);
});
for (const source of ['csrf401', 'csrfInactive', 'loginInvalid']) {
  test('FE1-LOGIN-STALE S1-S3 ' + source + ' clears password and explicitly retries with fresh token', async () => {
    const api = apiFixture(); window.history.replaceState({}, '', '/login');
    if (source !== 'loginInvalid') api.on('GET', '/api/auth/csrf', () => failure(source === 'csrf401' ? 401 : 403, source === 'csrf401' ? 'UNAUTHENTICATED' : 'ACCOUNT_INACTIVE'));
    else api.on('POST', '/api/auth/login', () => failure(403, 'CSRF_INVALID'));
    render(<App />); const user = userEvent.setup(); await submit(user);
    expect(await screen.findByRole('alert')).toHaveTextContent(/^Please try again\.$/);
    expect(screen.getByLabelText(/^password$/i)).toHaveValue('');
    expect(screen.getByRole('button', { name: /^sign in$/i })).toBeEnabled();
    expect(api.requests('/api/auth/login', 'POST')).toHaveLength(source === 'loginInvalid' ? 1 : 0);
    expect(api.requests('/api/auth/me')).toHaveLength(1);
    const count = api.requests('/api/auth/csrf').length;
    api.on('GET', '/api/auth/csrf', () => json({ headerName: 'X-FRESH', token: 'fresh-token' }));
    api.on('POST', '/api/auth/login', () => json(student));
    // Email survives, password is deliberately entered again by the user.
    await user.type(screen.getByLabelText(/^password$/i), 'Synthetic-password-12');
    await user.click(screen.getByRole('button', { name: /^sign in$/i }));
    await screen.findByRole('button', { name: /sign out/i });
    expect(api.requests('/api/auth/csrf').length).toBeGreaterThan(count);
    const posts = api.requests('/api/auth/login', 'POST');
    expect(posts).toHaveLength(source === 'loginInvalid' ? 2 : 1);
    expect(posts.at(-1)!.headers.get('X-FRESH')).toBe('fresh-token');
  });
}
for (const source of ['csrf', 'post']) for (const [status, code] of [[401, 'UNAUTHENTICATED'], [403, 'ACCOUNT_INACTIVE']] as const) {
  test(`FE1-LOGOUT-ENDED ${source} ${code} clears private state silently and refreshes token on later login`, async () => {
    const api = apiFixture(student);
    api.on(source === 'csrf' ? 'GET' : 'POST', source === 'csrf' ? '/api/auth/csrf' : '/api/auth/logout', () => failure(status, code));
    render(<App />); const user = userEvent.setup(); await screen.findByText(slot.tutorName);
    await user.click(screen.getByRole('button', { name: /sign out/i }));
    await screen.findByLabelText(/^email$/i);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /retry.*sign.out/i })).not.toBeInTheDocument();
    expect(screen.queryByText(slot.tutorName)).not.toBeInTheDocument(); expect(screen.queryByText(student.name)).not.toBeInTheDocument();
    expect(api.requests('/api/auth/me')).toHaveLength(1);
    expect(api.requests('/api/auth/logout', 'POST')).toHaveLength(source === 'csrf' ? 0 : 1);
    const count = api.requests('/api/auth/csrf').length;
    api.on('GET', '/api/auth/csrf', () => json({ headerName: 'X-FRESH', token: 'new-login-token' }));
    await submit(user); await screen.findByRole('button', { name: /sign out/i });
    expect(api.requests('/api/auth/csrf').length).toBeGreaterThan(count);
    expect(api.requests('/api/auth/login', 'POST')[0].headers.get('X-FRESH')).toBe('new-login-token');
  });
}
const unknown = '20000000-0000-4000-8000-000000000099';
test('FE2-UNKNOWN-MODULE S1 normalizes by replacement and rejects late removed-module results', async () => {
  const api = apiFixture(student); const old = deferred<Response>();
  const replace = vi.spyOn(window.history, 'replaceState');
  api.on('GET', '/api/student/slots', call => call.url.searchParams.has('moduleId') ? old.promise : json([slot]));
  window.history.replaceState({}, '', `/student/slots?moduleId=${unknown}&week=2026-10-19&date=2026-10-21`);
  render(<App />);
  await waitFor(() => expect(window.location.search).not.toContain('moduleId'));
  expect(window.location.search).toContain('week=2026-10-19'); expect(window.location.search).toContain('date=2026-10-21');
  expect(replace.mock.calls.some(call => String(call[2]).includes('week=2026-10-19') && !String(call[2]).includes('moduleId'))).toBe(true);
  expect(await screen.findByRole('alert')).toHaveTextContent(/Invalid filters were removed/);
  await screen.findByText(slot.tutorName);
  expect(screen.getByRole('combobox', { name: /module/i })).toHaveValue('');
  expect(api.requests('/api/student/slots').at(-1)!.url.searchParams.has('moduleId')).toBe(false);
  await act(async () => old.resolve(json([{ ...slot, tutorName: 'Stale unknown-module tutor' }])));
  expect(screen.queryByText('Stale unknown-module tutor')).not.toBeInTheDocument();
  await userEvent.setup().click(screen.getByRole('button', { name: /next week/i }));
  expect(screen.queryByText(/Invalid filters were removed/)).not.toBeInTheDocument();
  replace.mockRestore();
});
test('FE2-UNKNOWN-MODULE S2 listed module stays selected', async () => {
  const api = apiFixture(student); window.history.replaceState({}, '', `/student/slots?moduleId=${ids.module}&week=2026-10-19`);
  render(<App />); await screen.findByText(slot.tutorName);
  expect(screen.getByRole('combobox', { name: /module/i })).toHaveValue(ids.module);
  expect(api.requests('/api/student/slots').at(-1)!.url.searchParams.get('moduleId')).toBe(ids.module);
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});
for (const active of [false, true]) test.each(['503', 'network', 'malformed', '500'])('FE2-MODULES-DEGRADED S1-S3 active=' + active + ' modules %s leaves slots usable and Refresh recovers', async kind => {
  const api = apiFixture(student); api.on('GET', '/api/student/modules', () => bad(kind));
  if (active) window.history.replaceState({}, '', `/student/slots?moduleId=${unknown}&week=2026-10-19`);
  render(<App />);
  await screen.findByRole('button', { name: /^book$/i });
  const control = screen.getByRole('combobox', { name: /module/i }); expect(control).toBeDisabled();
  expect(screen.getByRole('status')).toHaveTextContent(/module filter.*unavailable/i);
  expect(screen.queryByRole('alert')).not.toBeInTheDocument(); expect(screen.queryByRole('button', { name: /^retry$/i })).not.toBeInTheDocument();
  if (active) {
    expect(window.location.search).toContain(unknown);
    expect(api.requests('/api/student/slots').at(-1)!.url.searchParams.get('moduleId')).toBe(unknown);
    expect((control as HTMLSelectElement).selectedOptions[0]?.textContent).not.toBe('All modules');
  }
  api.on('GET', '/api/student/modules', () => json([moduleOption, { ...moduleOption, id: unknown }]));
  await userEvent.setup().click(screen.getByRole('button', { name: /^refresh$/i }));
  await waitFor(() => expect(screen.getByRole('combobox', { name: /module/i })).toBeEnabled());
  expect(screen.queryByText(/module filter.*unavailable/i)).not.toBeInTheDocument();
});
test.each([[401, 'UNAUTHENTICATED'], [403, 'ACCOUNT_INACTIVE'], [403, 'FORBIDDEN']] as const)('FE2-MODULES-DEGRADED S4 modules %s %s retains denial handling', async (status, code) => {
  const api = apiFixture(student); api.on('GET', '/api/student/modules', () => failure(status, code)); render(<App />);
  if (code !== 'FORBIDDEN') await screen.findByLabelText(/^email$/i); else await screen.findByRole('alert');
  expect(screen.queryByRole('button', { name: /^book$/i })).not.toBeInTheDocument();
  expect(screen.queryByText(/module filter.*unavailable/i)).not.toBeInTheDocument();
});
test('FE2-MODULES-DEGRADED S4 slot failure remains a read error', async () => {
  const api = apiFixture(student); api.on('GET', '/api/student/slots', () => bad('503')); render(<App />);
  await screen.findByRole('alert'); expect(screen.getByRole('button', { name: /^retry$/i })).toBeEnabled();
  expect(screen.queryByRole('button', { name: /^book$/i })).not.toBeInTheDocument();
});
for (const kind of ['503', 'malformed']) for (const first of ['modules', 'slots']) {
  test(`FE4-R1 FE2-MODULES-DEGRADED S4 modules FORBIDDEN with slots ${kind}, ${first} completes first`, async () => {
    const api = apiFixture(student);
    const modules = deferred<Response>(); const slots = deferred<Response>();
    api.on('GET', '/api/student/modules', () => modules.promise);
    api.on('GET', '/api/student/slots', () => slots.promise);
    render(<App />);
    await waitFor(() => {
      expect(api.requests('/api/student/modules')).toHaveLength(1);
      expect(api.requests('/api/student/slots')).toHaveLength(1);
    });
    function expectDenied() {
      expect.soft(screen.getByRole('alert')).toHaveTextContent(/Access denied/i);
      expect.soft(screen.queryByRole('link', { name: /^back to home$/i })).toHaveAttribute('href', '/student/slots');
      expect.soft(screen.queryByRole('button', { name: /^retry$/i })).not.toBeInTheDocument();
      expect.soft(screen.queryByRole('complementary', { name: /consultation filters/i })).not.toBeInTheDocument();
      expect.soft(screen.queryByLabelText(/^module$/i)).not.toBeInTheDocument();
      expect.soft(screen.queryByRole('region', { name: /available consultations/i })).not.toBeInTheDocument();
      expect.soft(screen.queryByRole('button', { name: /^book$/i })).not.toBeInTheDocument();
    }
    if (first === 'modules') {
      await act(async () => modules.resolve(failure(403, 'FORBIDDEN')));
      expectDenied();
      await act(async () => slots.resolve(bad(kind)));
    } else {
      await act(async () => slots.resolve(bad(kind)));
      expect(screen.getByRole('button', { name: /^retry$/i })).toBeEnabled();
      await act(async () => modules.resolve(failure(403, 'FORBIDDEN')));
    }
    expectDenied();
    expect(screen.getByRole('button', { name: /sign out/i })).toBeEnabled();
    expect(screen.queryByLabelText(/^email$/i)).not.toBeInTheDocument();
    expect(api.requests('/api/auth/me')).toHaveLength(1);
    expect(api.calls.filter(call => call.method === 'POST')).toHaveLength(0);
  });
}
test.each([320, 1440])('FE2-BROWSE-MIDNIGHT %spx shows one slot with next-day end date and 00:30 SGT', async width => {
  vi.stubGlobal('innerWidth', width); const api = apiFixture(student);
  api.givenSlots([{ ...slot, startTime: '2026-10-25T15:30:00Z', endTime: '2026-10-25T16:30:00Z' }]); render(<App />);
  await screen.findByText(slot.tutorName);
  expect(screen.getAllByText(slot.tutorName)).toHaveLength(1);
  expect(document.body).toHaveTextContent(/26 Oct/); expect(document.body).toHaveTextContent('00:30'); expect(document.body).toHaveTextContent('SGT');
});

