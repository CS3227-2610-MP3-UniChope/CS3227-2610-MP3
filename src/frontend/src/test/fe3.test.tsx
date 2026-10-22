import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import App from '../App';
import { apiFixture, deferred, failure, ids, json, slot, student } from './api-fixture';

beforeEach(() => window.history.replaceState({}, '', '/student/slots?week=2026-10-19'));
afterEach(() => vi.unstubAllGlobals());
const cases = ['503', 'network', 'malformed', 'unexpected'] as const;
function bad(kind: typeof cases[number]) {
  if (kind === 'network') throw new TypeError('Synthetic private diagnostic');
  if (kind === '503') return failure(503, 'TEMPORARILY_UNAVAILABLE');
  if (kind === 'unexpected') return json({}, 202);
  return json({ token: 4 });
}
const operations = ['book', 'cancel'] as const;
function mutationPath(operation: typeof operations[number]) {
  return operation === 'book' ? '/api/student/bookings' : `/api/student/bookings/${ids.booking}/cancel`;
}
async function open(operation: typeof operations[number], user: ReturnType<typeof userEvent.setup>) {
  await user.click(await screen.findByRole('button', { name: operation === 'book' ? /^book$/i : /^cancel(?: booking)?$/i }));
  const dialog = await screen.findByRole('dialog');
  expect(dialog).toHaveTextContent(slot.tutorName);
  return within(dialog).getByRole('button', { name: /confirm/i });
}
for (const operation of operations) {
  test.each(cases)('FE2-NOTHING-SENT S1/S2 ' + operation + ' CSRF %s sends nothing and retries through confirmation', async kind => {
    const api = apiFixture(student);
    api.on('GET', '/api/auth/csrf', () => bad(kind));
    if (operation === 'cancel') window.history.replaceState({}, '', '/student/bookings');
    render(<App />);
    const user = userEvent.setup();
    await user.click(await open(operation, user));
    expect(api.requests(mutationPath(operation), 'POST')).toHaveLength(0);
    expect(await screen.findByRole('alert')).toHaveTextContent(/^Nothing was changed\. Please try again\./);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(document.body).not.toHaveTextContent(/may have completed|check My bookings/i);
    expect(document.body).not.toHaveTextContent('Synthetic private diagnostic');
    api.on('GET', '/api/auth/csrf', () => json({ headerName: 'X-FRESH-CSRF', token: 'fresh-retry-token' }));
    await user.click(screen.getByRole('button', { name: /^retry$/i }));
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent(slot.moduleCode);
    expect(dialog).toHaveTextContent(slot.tutorName);
    expect(api.requests(mutationPath(operation), 'POST')).toHaveLength(0);
    await user.click(within(dialog).getByRole('button', { name: /confirm/i }));
    await screen.findByText(operation === 'book' ? /booking confirmed/i : /booking cancelled/i);
    const posts = api.requests(mutationPath(operation), 'POST');
    expect(posts).toHaveLength(1);
    expect(posts[0].headers.get('X-FRESH-CSRF')).toBe('fresh-retry-token');
    expect(posts[0].body).toEqual(operation === 'book' ? { slotId: ids.slot } : {});
    expect(api.requests('/api/auth/csrf')).toHaveLength(2);
  });

  test.each(['network', '503', 'malformed'] as const)('FE2-NOTHING-SENT S3 ' + operation + ' sent POST %s remains uncertain without replay', async kind => {
    const api = apiFixture(student);
    api.on('POST', mutationPath(operation), () => kind === 'malformed' ? json({}) : bad(kind));
    if (operation === 'cancel') window.history.replaceState({}, '', '/student/bookings');
    render(<App />);
    const user = userEvent.setup();
    await user.click(await open(operation, user));
    expect(await screen.findByRole('alert')).toHaveTextContent(/may have completed.*Refresh and check My bookings/i);
    expect(document.body).not.toHaveTextContent('Nothing was changed.');
    expect(api.requests(mutationPath(operation), 'POST')).toHaveLength(1);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  test.each([[401, 'UNAUTHENTICATED'], [403, 'ACCOUNT_INACTIVE']] as const)('FE1-DENIAL-CACHED ' + operation + ' POST %s %s clears both private pages without replay', async (status, code) => {
    const api = apiFixture();
    window.history.replaceState({}, '', '/login');
    render(<App />);
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/^email$/i), 'synthetic@example.test');
    await user.type(screen.getByLabelText(/^password$/i), 'Synthetic-password-12');
    await user.click(screen.getByRole('button', { name: /^sign in$/i }));
    // Login rotates and acquires the protected token, exercising the cached POST path.
    await screen.findByRole('button', { name: /^book$/i });
    await user.click(screen.getByRole('link', { name: /my bookings/i }));
    await screen.findByRole('button', { name: /^cancel(?: booking)?$/i });
    if (operation === 'book') await user.click(screen.getByRole('link', { name: /find a consultation/i }));
    const tokenReads = api.requests('/api/auth/csrf').length;
    api.on('POST', mutationPath(operation), () => failure(status, code));
    await user.click(await open(operation, user));
    await screen.findByLabelText(/^email$/i);
    expect(await screen.findByRole('alert')).toHaveTextContent(code === 'ACCOUNT_INACTIVE' ? /inactive/i : /session expired/i);
    expect(screen.getByRole('alert')).not.toHaveTextContent(code === 'ACCOUNT_INACTIVE' ? /session expired/i : /inactive/i);
    expect(screen.queryByText(student.name)).not.toBeInTheDocument();
    expect(screen.queryByText(slot.tutorName)).not.toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^book$|^cancel(?: booking)?$/i })).not.toBeInTheDocument();
    expect(api.requests(mutationPath(operation), 'POST')).toHaveLength(1);
    expect(api.requests('/api/auth/csrf')).toHaveLength(tokenReads);
    expect(api.requests('/api/auth/me')).toHaveLength(1);
  });
}

for (const role of ['STUDENT', 'TUTOR', 'ADMIN'] as const) {
  test.each(['503', 'network', 'malformed'] as const)('FE1-LOGOUT-CONFIRMED S1/S2 ' + role + ' anonymous CSRF %s cannot undo 204', async kind => {
    const api = apiFixture({ ...student, role });
    let signedOut = false;
    api.on('GET', '/api/auth/csrf', () => signedOut ? bad(kind) : json({ headerName: 'X-TEST-CSRF', token: 'logout-token' }));
    api.on('POST', '/api/auth/logout', () => { signedOut = true; return new Response(null, { status: 204 }); });
    render(<App />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /sign out/i }));
    await waitFor(() => expect(screen.getByRole('button', { name: /^sign in$/i })).toBeEnabled());
    expect(api.requests('/api/auth/logout', 'POST')).toHaveLength(1);
    expect(api.requests('/api/auth/csrf')).toHaveLength(2);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /retry.*sign.out/i })).not.toBeInTheDocument();
    expect(screen.queryByText(student.name)).not.toBeInTheDocument();
    expect(screen.queryByText(slot.tutorName)).not.toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    api.on('GET', '/api/auth/csrf', () => json({ headerName: 'X-FRESH-CSRF', token: 'fresh-login-token' }));
    await user.type(screen.getByLabelText(/^email$/i), 'synthetic@example.test');
    await user.type(screen.getByLabelText(/^password$/i), 'Synthetic-password-12');
    await user.click(screen.getByRole('button', { name: /^sign in$/i }));
    await screen.findByRole('button', { name: /sign out/i });
    const post = api.requests('/api/auth/login', 'POST');
    expect(post).toHaveLength(1);
    expect(post[0].headers.get('X-FRESH-CSRF')).toBe('fresh-login-token');
    expect(api.calls.indexOf(api.requests('/api/auth/csrf')[2])).toBeLessThan(api.calls.indexOf(post[0]));
    expect(api.requests('/api/auth/me')).toHaveLength(1);
  });
}

test.each(['network', '503', 'malformed', 'unexpected', 'csrf-invalid'] as const)('FE1-LOGOUT-CONFIRMED S3 logout POST %s remains unconfirmed and retryable', async kind => {
  const api = apiFixture(student);
  api.on('POST', '/api/auth/logout', () => kind === 'csrf-invalid' ? failure(403, 'CSRF_INVALID') : kind === 'malformed' ? new Response('broken', { status: 200 }) : bad(kind));
  render(<App />);
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /sign out/i }));
  expect(await screen.findByRole('alert')).toHaveTextContent(/server sign-out could not be confirmed/i);
  expect(screen.getByRole('button', { name: /retry sign out/i })).toBeEnabled();
  expect(screen.queryByText(student.name)).not.toBeInTheDocument();
  expect(api.requests('/api/auth/me')).toHaveLength(1);
  expect(api.requests('/api/auth/logout', 'POST')).toHaveLength(1);
});

function landmarks() {
  expect(screen.getAllByRole('banner')).toHaveLength(1);
  expect(screen.getByRole('banner')).toHaveTextContent('UniChope');
  expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
  expect(screen.getByRole('link', { name: /skip/i })).toHaveAttribute('href', '#main-content');
}
test.each(['login', 'restoring', 'restore-failure', 'not-found', 'slots', 'bookings', 'TUTOR', 'ADMIN'] as const)('FE1-UI-LANDMARKS S1/S2/S4 %s has one banner, h1 and skip link', async page => {
  const api = apiFixture(page === 'login' ? null : { ...student, role: page === 'TUTOR' || page === 'ADMIN' ? page : 'STUDENT' });
  if (page === 'restoring') api.on('GET', '/api/auth/me', () => deferred<Response>().promise);
  if (page === 'restore-failure') api.on('GET', '/api/auth/me', () => failure(503, 'TEMPORARILY_UNAVAILABLE'));
  const path = page === 'not-found' ? '/missing' : page === 'bookings' ? '/student/bookings' : page === 'login' ? '/login' : page === 'TUTOR' || page === 'ADMIN' ? `/${page.toLowerCase()}` : '/student/slots?week=2026-10-19';
  window.history.replaceState({}, '', path);
  render(<App />);
  if (page === 'login') await screen.findByLabelText(/^email$/i);
  else if (page === 'restore-failure') await screen.findByRole('alert');
  else if (page === 'not-found') await screen.findByRole('heading', { name: /not found/i });
  else if (page !== 'restoring') await screen.findByRole('button', { name: /sign out/i });
  landmarks();
  if (page === 'TUTOR' || page === 'ADMIN') {
    const banner = screen.getByRole('banner');
    expect(banner).toHaveTextContent('UniChope');
    expect(within(banner).getByRole('button', { name: 'Dark theme' })).toBeVisible();
    expect(banner.textContent?.replace(/UniChope|Dark theme|Sign out/g, '').trim()).toBe('');
    expect(within(banner).getAllByRole('button')).toHaveLength(2);
    expect(within(banner).getByRole('button', { name: /^sign out$/i })).toBeVisible();
    expect(within(banner).queryAllByRole('link').length).toBeLessThanOrEqual(1);
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
    expect(banner).not.toHaveTextContent(student.name);
  }
  if (page === 'slots' || page === 'bookings') {
    expect(screen.getByRole('banner')).toHaveTextContent(student.name);
    expect(screen.getByRole('banner')).toHaveTextContent('Student');
    expect(screen.getByRole('navigation', { name: /student/i })).toBeVisible();
  }
});
test('FE1-UI-LANDMARKS S3 Login begins with Sign in h1 without skipped heading levels', async () => {
  apiFixture();
  window.history.replaceState({}, '', '/login');
  render(<App />);
  await screen.findByLabelText(/^email$/i);
  const headings = screen.getAllByRole('heading');
  expect(headings[0]).toHaveTextContent(/^Sign in$/);
  expect(headings[0].tagName).toBe('H1');
  for (let index = 1; index < headings.length; index++) expect(Number(headings[index].tagName[1])).toBeLessThanOrEqual(Number(headings[index - 1].tagName[1]) + 1);
});

const notice = /Invalid filters were removed/i;
async function visit(path: string) {
  await act(async () => {
    window.history.pushState({}, '', path);
    window.dispatchEvent(new PopStateEvent('popstate'));
  });
}
test.each(['module', 'date', 'previous', 'this', 'next', 'reset', 'status', 'upcoming', 'history', 'page', 'popstate'] as const)('FE2-NOTICE-CLEAR S1/S2/S4 %s clears normalization notice and later invalid navigation restores it', async action => {
  apiFixture(student);
  const bookingPage = ['status', 'upcoming', 'history'].includes(action);
  const path = bookingPage ? '/student/bookings?view=history&status=wrong' : '/student/slots?week=2026-10-19&bad=1';
  window.history.replaceState({}, '', path);
  render(<App />);
  await screen.findByText(notice);
  await screen.findByText(slot.tutorName);
  await waitFor(() => expect(window.location.search).not.toMatch(/bad=|wrong/));
  expect(screen.getByText(notice)).toBeVisible();
  const user = userEvent.setup();
  if (action === 'module') await user.selectOptions(screen.getByRole('combobox', { name: /^module$/i }), ids.module);
  else if (action === 'date') fireEvent.change(screen.getByLabelText(/specific date/i), { target: { value: '2026-10-21' } });
  else if (action === 'status') await user.selectOptions(screen.getByRole('combobox', { name: /^status$/i }), 'CANCELLED');
  else if (action === 'page') await user.click(screen.getByRole('link', { name: /my bookings/i }));
  else if (action === 'popstate') await visit('/student/slots?week=2026-10-26');
  else await user.click(screen.getByRole('button', { name: action === 'reset' ? /^reset$/i : action === 'upcoming' ? /^upcoming$/i : action === 'history' ? /^history$/i : new RegExp(`${action} week`, 'i') }));
  await waitFor(() => expect(screen.queryByText(notice)).not.toBeInTheDocument());
  await visit(path);
  expect(await screen.findByText(notice)).toBeVisible();
});
test.each(['Refresh', 'Retry'])('FE2-NOTICE-CLEAR S3 read %s keeps normalization notice', async action => {
  const api = apiFixture(student);
  if (action === 'Retry') api.on('GET', '/api/student/slots', () => failure(503, 'TEMPORARILY_UNAVAILABLE'));
  window.history.replaceState({}, '', '/student/slots?week=2026-10-19&bad=1');
  render(<App />);
  await screen.findByText(notice);
  if (action === 'Retry') await screen.findByRole('button', { name: /^retry$/i });
  else await screen.findByText(slot.tutorName);
  const count = api.requests('/api/student/slots').length;
  api.on('GET', '/api/student/slots', () => json([slot]));
  await userEvent.setup().click(screen.getByRole('button', { name: new RegExp(`^${action}$`, 'i') }));
  await waitFor(() => expect(api.requests('/api/student/slots').length).toBeGreaterThan(count));
  await screen.findByText(slot.tutorName);
  expect(screen.getByText(notice)).toBeVisible();
});
