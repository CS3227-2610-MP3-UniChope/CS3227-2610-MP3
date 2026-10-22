import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import App from '../App';
import { apiFixture, booking, deferred, failure, ids, json, slot, student } from './api-fixture';

beforeEach(() => { window.history.replaceState({}, '', '/student/slots?week=2026-10-19'); });
afterEach(() => { vi.unstubAllGlobals(); });
async function browse() { return screen.findByRole('button', { name: /^book(?: consultation)?$/i }); }
const bookingsPath = '/api/student/bookings';
async function confirmBook(user: ReturnType<typeof userEvent.setup>) {
  await user.click(await browse());
  const dialog = await screen.findByRole('dialog');
  await user.click(within(dialog).getByRole('button', { name: /confirm/i }));
}
function expectSafeQueries(calls: ReturnType<typeof apiFixture>['calls']) {
  for (const call of calls.filter(call => call.url.pathname === '/api/student/slots')) {
    expect([...call.url.searchParams.keys()].every(key => ['moduleId', 'date'].includes(key))).toBe(true);
  }
}

test('FE2-BROWSE seven-day week preserves overlapping and cross-midnight SGT choices without unsupported queries', async () => {
  const api = apiFixture(student);
  api.givenSlots([
    { ...slot, id: ids.slot, tutorName: 'Early Monday Tutor', startTime: '2026-10-18T16:05:00Z', endTime: '2026-10-18T17:00:00Z' },
    { ...slot, id: ids.otherSlot, tutorName: 'Overlapping Monday Tutor', startTime: '2026-10-18T16:05:00Z', endTime: '2026-10-18T17:30:00Z' },
    { ...slot, id: '30000000-0000-4000-8000-000000000003', tutorName: 'Late Sunday Tutor', startTime: '2026-10-25T15:30:00Z', endTime: '2026-10-25T16:30:00Z' },
    { ...slot, id: '30000000-0000-4000-8000-000000000004', tutorName: 'Outside This Week', startTime: '2026-10-25T16:05:00Z', endTime: '2026-10-25T17:00:00Z' },
  ]);
  render(<App />);
  await screen.findByText('Early Monday Tutor');
  expect(screen.getByText('Overlapping Monday Tutor')).toBeVisible();
  expect(screen.getByText('Late Sunday Tutor')).toBeVisible();
  expect(screen.queryByText('Outside This Week')).not.toBeInTheDocument();
  expect(screen.getAllByRole('button', { name: /^book(?: consultation)?$/i })).toHaveLength(3);
  expect(document.body).toHaveTextContent(/SGT/);
  expect(document.body).toHaveTextContent(/Monday|Mon/);
  expect(document.body).toHaveTextContent(/Sunday|Sun/);
  expectSafeQueries(api.calls);
  expect(api.requests('/api/student/modules')[0].url.search).toBe('');
});

test('FE2-BROWSE module/date URL maps exact query; next week clears outside date; popstate restores', async () => {
  const api = apiFixture(student);
  window.history.replaceState({}, '', `/student/slots?moduleId=${ids.module}&date=2026-10-21&week=2026-10-19`);
  render(<App />);
  await browse();
  let read = api.requests('/api/student/slots').at(-1)!;
  expect(read.url.searchParams.get('moduleId')).toBe(ids.module);
  expect(read.url.searchParams.get('date')).toBe('2026-10-21');
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: /next week/i }));
  await waitFor(() => expect(new URLSearchParams(window.location.search).get('week')).toBe('2026-10-26'));
  expect(new URLSearchParams(window.location.search).has('date')).toBe(false);
  await act(async () => {
    window.history.pushState({}, '', `/student/slots?moduleId=${ids.module}&date=2026-10-21&week=2026-10-19`);
    window.dispatchEvent(new PopStateEvent('popstate'));
  });
  await browse();
  read = api.requests('/api/student/slots').at(-1)!;
  expect(read.url.searchParams.get('date')).toBe('2026-10-21');
  expectSafeQueries(api.calls);
});

test('FE2-BROWSE invalid/unknown URL filters are announced and never forwarded', async () => {
  const api = apiFixture(student);
  window.history.replaceState({}, '', '/student/slots?moduleId=wrong&date=2026-02-30&week=nope&actorId=secret');
  render(<App />);
  expect(await screen.findByRole('alert')).toHaveTextContent(/filter|invalid/i);
  await waitFor(() => expect(api.requests('/api/student/slots').length).toBeGreaterThan(0));
  expectSafeQueries(api.calls);
  const read = api.requests('/api/student/slots').at(-1)!;
  expect(read.url.searchParams.has('moduleId')).toBe(false);
  expect(read.url.searchParams.has('date')).toBe(false);
  expect(window.location.search).not.toContain('actorId');
});

test('FE2-BROWSE module control changes exact filter and empty state remains refreshable', async () => {
  const api = apiFixture(student);
  api.on('GET', '/api/student/slots', call => json(call.url.searchParams.has('moduleId') ? [] : [slot]));
  render(<App />);
  await browse();
  const user = userEvent.setup();
  await user.selectOptions(screen.getByRole('combobox', { name: /module/i }), ids.otherModule);
  await waitFor(() => expect(api.requests('/api/student/slots').at(-1)!.url.searchParams.get('moduleId')).toBe(ids.otherModule));
  await screen.findByText(/no.*(?:slots|consultations)|nothing.*match/i);
  expect(screen.queryByRole('button', { name: /^book$/i })).not.toBeInTheDocument();
  const count = api.requests('/api/student/slots').length;
  await user.click(screen.getByRole('button', { name: /refresh/i }));
  await waitFor(() => expect(api.requests('/api/student/slots').length).toBeGreaterThan(count));
});

test('FE2-BROWSE read error has explicit retry and no stale bookable results', async () => {
  const api = apiFixture(student);
  api.on('GET', '/api/student/slots', () => failure(503, 'TEMPORARILY_UNAVAILABLE'));
  render(<App />);
  await screen.findByRole('alert');
  expect(screen.queryByRole('button', { name: /^book$/i })).not.toBeInTheDocument();
  api.on('GET', '/api/student/slots', () => json([slot]));
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: /retry|try again/i }));
  await browse();
});

test('FE2-BOOK accessible confirmation can Escape/Keep without POST and restores trigger focus', async () => {
  const api = apiFixture(student);
  render(<App />);
  const trigger = await browse();
  const user = userEvent.setup();
  await user.click(trigger);
  const dialog = await screen.findByRole('dialog');
  expect(dialog).toHaveAccessibleName();
  expect(dialog).toHaveTextContent(slot.moduleCode);
  expect(dialog).toHaveTextContent(slot.tutorName);
  expect(dialog).toHaveTextContent(/SGT/);
  expect(dialog).toContainElement(document.activeElement as HTMLElement);
  await user.keyboard('{Escape}');
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(trigger).toHaveFocus();
  await user.click(trigger);
  await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: /keep|back|cancel/i }));
  expect(api.requests(bookingsPath, 'POST')).toHaveLength(0);
});

test('FE2-BOOK confirm submits exact slotId once with CSRF, awaits success and refreshes loaded views', async () => {
  const api = apiFixture(student);
  const pending = deferred<Response>();
  api.on('POST', bookingsPath, () => pending.promise);
  render(<App />);
  const user = userEvent.setup();
  await confirmBook(user);
  const post = api.requests(bookingsPath, 'POST');
  expect(post).toHaveLength(1);
  expect(post[0].body).toEqual({ slotId: ids.slot });
  expect(post[0].headers.get('X-TEST-CSRF')).toMatch(/^csrf-/);
  expect(post[0].headers.get('Content-Type')).toMatch(/application\/json/);
  expect(screen.queryByText(/booking confirmed/i)).not.toBeInTheDocument();
  const confirm = within(screen.getByRole('dialog')).getByRole('button', { name: /confirm/i });
  expect(confirm).toBeDisabled();
  await user.click(confirm);
  expect(api.requests(bookingsPath, 'POST')).toHaveLength(1);
  const count = api.requests('/api/student/slots').length;
  api.givenSlots([]);
  await act(async () => pending.resolve(json(booking, 201)));
  await screen.findByText(/booking confirmed|successfully booked/i);
  await waitFor(() => expect(api.requests('/api/student/slots').length).toBeGreaterThan(count));
  await user.click(screen.getByRole('link', { name: /my bookings/i }));
  await screen.findByText(slot.tutorName);
  expect(api.requests(bookingsPath, 'GET').at(-1)!.url.searchParams.getAll('status')).toEqual(['ACTIVE']);
});

test.each([
  ['SLOT_UNAVAILABLE', /no longer available|unavailable/i],
  ['BOOKING_OVERLAP', /overlap|another.*booking/i],
] as const)('FE2-BOOK %s has distinct safe guidance, refresh and no replay', async (code, message) => {
  const api = apiFixture(student);
  api.on('POST', bookingsPath, () => failure(409, code));
  render(<App />);
  const user = userEvent.setup();
  await browse();
  const count = api.requests('/api/student/slots').length;
  await confirmBook(user);
  expect(await screen.findByRole('alert')).toHaveTextContent(message);
  expect(api.requests(bookingsPath, 'POST')).toHaveLength(1);
  await waitFor(() => expect(api.requests('/api/student/slots').length).toBeGreaterThan(count));
});

test('FE2-BOOK lost response explains uncertain outcome and offers refresh/check bookings without replay', async () => {
  const api = apiFixture(student);
  api.on('POST', bookingsPath, () => { throw new TypeError('response lost after commit'); });
  render(<App />);
  await confirmBook(userEvent.setup());
  expect(await screen.findByRole('alert')).toHaveTextContent(/confirm|uncertain|check.*bookings|may have/i);
  expect(screen.getByRole('link', { name: /my bookings/i })).toBeVisible();
  expect(api.requests(bookingsPath, 'POST')).toHaveLength(1);
});

test('FE2-LIST Upcoming includes started ACTIVE, preserves server ordering and honours cancellable only', async () => {
  const api = apiFixture(student);
  api.givenBookings([
    { ...booking, tutorName: 'Already Started', startTime: '2020-01-01T00:00:00Z', endTime: '2020-01-01T01:00:00Z', cancellable: false },
    { ...booking, id: '40000000-0000-4000-8000-000000000002', tutorName: 'Server Allows Cancellation', startTime: '2020-01-02T00:00:00Z', cancellable: true },
  ]);
  window.history.replaceState({}, '', '/student/bookings');
  render(<App />);
  await screen.findByText('Already Started');
  expect(screen.getByText('Server Allows Cancellation')).toBeVisible();
  expect(screen.getAllByRole('button', { name: /^cancel(?: booking)?$/i })).toHaveLength(1);
  expect(document.body.textContent!.indexOf('Already Started')).toBeLessThan(document.body.textContent!.indexOf('Server Allows Cancellation'));
  expect(api.requests(bookingsPath, 'GET')[0].url.searchParams.getAll('status')).toEqual(['ACTIVE']);
});

test('FE2-LIST History uses repeatable statuses and exact SGT date, status filter is server-driven', async () => {
  const api = apiFixture(student);
  api.givenBookings([{ ...booking, status: 'CANCELLED', cancellable: false }]);
  window.history.replaceState({}, '', '/student/bookings?view=history&date=2026-10-21');
  render(<App />);
  await screen.findByText(slot.tutorName);
  const query = api.requests(bookingsPath, 'GET').at(-1)!.url.searchParams;
  expect(query.getAll('status').sort()).toEqual(['CANCELLED', 'COMPLETED']);
  expect(query.get('date')).toBe('2026-10-21');
  expect(screen.queryByRole('button', { name: /^cancel(?: booking)?$/i })).not.toBeInTheDocument();
  const user = userEvent.setup();
  await user.selectOptions(screen.getByRole('combobox', { name: /status/i }), 'COMPLETED');
  await waitFor(() => expect(api.requests(bookingsPath, 'GET').at(-1)!.url.searchParams.getAll('status')).toEqual(['COMPLETED']));
  expect(new URLSearchParams(window.location.search).get('status')).toBe('COMPLETED');
});

test('FE2-CANCEL cancellation submits exact empty body once after confirmation and refreshes bookings', async () => {
  const api = apiFixture(student);
  window.history.replaceState({}, '', '/student/bookings');
  const pending = deferred<Response>();
  const path = `${bookingsPath}/${ids.booking}/cancel`;
  api.on('POST', path, () => pending.promise);
  render(<App />);
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /^cancel(?: booking)?$/i }));
  let dialog = await screen.findByRole('dialog');
  expect(dialog).toHaveTextContent(slot.moduleCode);
  expect(dialog).toHaveTextContent(slot.tutorName);
  expect(dialog).toHaveTextContent(/SGT/);
  await user.click(within(dialog).getByRole('button', { name: /keep/i }));
  expect(api.requests(path, 'POST')).toHaveLength(0);
  await user.click(screen.getByRole('button', { name: /^cancel(?: booking)?$/i }));
  dialog = await screen.findByRole('dialog');
  await user.click(within(dialog).getByRole('button', { name: /confirm/i }));
  expect(api.requests(path, 'POST')).toHaveLength(1);
  expect(api.requests(path, 'POST')[0].body).toEqual({});
  expect(api.requests(path, 'POST')[0].headers.get('X-TEST-CSRF')).toMatch(/^csrf-/);
  expect(within(dialog).getByRole('button', { name: /confirm/i })).toBeDisabled();
  const count = api.requests(bookingsPath, 'GET').length;
  api.givenBookings([]);
  await act(async () => pending.resolve(json({ ...booking, status: 'CANCELLED', cancellable: false })));
  await screen.findByText(/booking cancelled|successfully cancelled/i);
  await waitFor(() => expect(api.requests(bookingsPath, 'GET').length).toBeGreaterThan(count));
});

test.each([[404, 'NOT_FOUND'], [409, 'INVALID_STATE']] as const)('FE2-CANCEL %s safe stale result refreshes without replay', async (status, code) => {
  const api = apiFixture(student);
  const path = `${bookingsPath}/${ids.booking}/cancel`;
  api.on('POST', path, () => failure(status, code));
  window.history.replaceState({}, '', '/student/bookings');
  render(<App />);
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /^cancel(?: booking)?$/i }));
  const count = api.requests(bookingsPath, 'GET').length;
  await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: /confirm/i }));
  expect(await screen.findByRole('alert')).toHaveTextContent(/no longer|unavailable|changed|cancel/i);
  await waitFor(() => expect(api.requests(bookingsPath, 'GET').length).toBeGreaterThan(count));
  expect(api.requests(path, 'POST')).toHaveLength(1);
});

test.each([[401, 'UNAUTHENTICATED'], [403, 'ACCOUNT_INACTIVE']] as const)('FE1-DENIAL/FE2-RACE %s %s clears protected state to Login', async (status, code) => {
  const api = apiFixture(student);
  api.on('GET', '/api/student/slots', () => failure(status, code));
  render(<App />);
  await screen.findByLabelText(/^email$/i);
  expect(screen.queryByText(student.name)).not.toBeInTheDocument();
  expect(await screen.findByRole('alert')).toHaveTextContent(/session|sign in|inactive/i);
  expect(screen.queryByRole('button', { name: /^book$/i })).not.toBeInTheDocument();
});

test('FE1-DENIAL protected FORBIDDEN offers home/sign-out without pretending server logout', async () => {
  const api = apiFixture(student);
  api.on('GET', '/api/student/slots', () => failure(403, 'FORBIDDEN'));
  render(<App />);
  expect(await screen.findByRole('alert')).toHaveTextContent(/access|permission|denied/i);
  expect(screen.getByRole('button', { name: /sign out/i })).toBeVisible();
  expect(screen.getByRole('link', { name: /home/i })).toBeVisible();
  expect(api.requests('/api/auth/logout', 'POST')).toHaveLength(0);
  expect(screen.queryByRole('button', { name: /^book$/i })).not.toBeInTheDocument();
});

test('FE1-DENIAL CSRF_INVALID acquires new token only for explicit retry, never replays mutation', async () => {
  const api = apiFixture(student);
  api.on('POST', bookingsPath, () => failure(403, 'CSRF_INVALID'));
  render(<App />);
  const user = userEvent.setup();
  await confirmBook(user);
  expect(await screen.findByRole('alert')).toHaveTextContent(/security|token|session|retry/i);
  expect(api.requests(bookingsPath, 'POST')).toHaveLength(1);
  const oldToken = api.requests(bookingsPath, 'POST')[0].headers.get('X-TEST-CSRF');
  api.on('POST', bookingsPath, () => json(booking, 201));
  await user.click(screen.getByRole('button', { name: /retry|try again/i }));
  // An explicit retry may reopen confirmation; it must never bypass a fresh token.
  const dialog = screen.queryByRole('dialog');
  if (dialog && api.requests(bookingsPath, 'POST').length === 1) {
    await user.click(within(dialog).getByRole('button', { name: /confirm/i }));
  }
  await waitFor(() => expect(api.requests(bookingsPath, 'POST')).toHaveLength(2));
  expect(api.requests(bookingsPath, 'POST')[1].headers.get('X-TEST-CSRF')).not.toBe(oldToken);
});

test('FE2-RACE late filter read cannot replace current module results', async () => {
  const api = apiFixture(student);
  const old = deferred<Response>();
  api.on('GET', '/api/student/slots', call => call.url.searchParams.get('moduleId') === ids.otherModule
    ? json([{ ...slot, tutorName: 'Current Filter Tutor' }]) : old.promise);
  render(<App />);
  const control = await screen.findByRole('combobox', { name: /module/i });
  const user = userEvent.setup();
  await user.selectOptions(control, ids.otherModule);
  await screen.findByText('Current Filter Tutor');
  await act(async () => old.resolve(json([{ ...slot, tutorName: 'Stale Filter Tutor' }])));
  expect(screen.queryByText('Stale Filter Tutor')).not.toBeInTheDocument();
  expect(screen.getByText('Current Filter Tutor')).toBeVisible();
});

test('FE1-LOGOUT/PRIVACY FE2-RACE late mutation cannot resurrect old actor after logout/new Login', async () => {
  const api = apiFixture(student);
  const pending = deferred<Response>();
  api.on('POST', bookingsPath, () => pending.promise);
  render(<App />);
  const user = userEvent.setup();
  await confirmBook(user);
  // Sign-out remains operable even while an operation is outstanding.
  await user.click(screen.getByRole('button', { name: /sign out/i }));
  await screen.findByLabelText(/^email$/i);
  api.on('POST', '/api/auth/login', () => json({ ...student, name: 'New Admin', role: 'ADMIN' }));
  await user.type(screen.getByLabelText(/^email$/i), 'new@example.test');
  await user.type(screen.getByLabelText(/^password$/i), 'Synthetic-password-12');
  await user.click(screen.getByRole('button', { name: /^sign in$/i }));
  await screen.findByRole('heading', { level: 1, name: /admin/i });
  const reads = api.requests('/api/student/slots').length;
  await act(async () => pending.resolve(json(booking, 201)));
  expect(screen.queryByText(/booking confirmed/i)).not.toBeInTheDocument();
  expect(screen.queryByText(slot.tutorName)).not.toBeInTheDocument();
  expect(api.requests('/api/student/slots')).toHaveLength(reads);
  expect(screen.getByRole('heading', { level: 1, name: /admin/i })).toBeVisible();
  expect(screen.queryByText(student.name)).not.toBeInTheDocument();
});

test('FE2-UI no unfinished AI route/actions are exposed in booking workspace', async () => {
  apiFixture(student);
  render(<App />);
  await browse();
  expect(screen.queryByRole('button', { name: /agenda|generate|AI assistant/i })).not.toBeInTheDocument();
  expect(screen.queryByRole('link', { name: /agenda|generate|AI assistant/i })).not.toBeInTheDocument();
});

test('FE2-RACE popstate during a pending mutation discards its completion and leaves a new confirmation usable', async () => {
  const api = apiFixture(student);
  const oldMutation = deferred<Response>();
  api.on('POST', bookingsPath, call => call.body && (call.body as { slotId: string }).slotId === ids.slot
    ? oldMutation.promise : json({ ...booking, slotId: ids.otherSlot }, 201));
  api.on('GET', '/api/student/slots', call => json(call.url.searchParams.get('moduleId') === ids.otherModule
    ? [{ ...slot, id: ids.otherSlot, moduleId: ids.otherModule, tutorName: 'New Navigation Tutor' }] : [slot]));
  render(<App />);
  const user = userEvent.setup();
  await confirmBook(user);
  await waitFor(() => expect(api.requests(bookingsPath, 'POST')).toHaveLength(1));
  expect(within(screen.getByRole('dialog')).getByRole('button', { name: /confirm/i })).toBeDisabled();

  // Back/forward dispatches popstate even when a modal is submitting. This
  // changes selection context without changing actor or unmounting the App.
  await act(async () => {
    window.history.pushState({}, '', `/student/slots?moduleId=${ids.otherModule}&week=2026-10-19`);
    window.dispatchEvent(new PopStateEvent('popstate'));
  });
  await screen.findByText('New Navigation Tutor');
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  await act(async () => oldMutation.resolve(json(booking, 201)));
  expect(screen.queryByText(/booking confirmed/i)).not.toBeInTheDocument();
  expect(api.requests(bookingsPath, 'POST')).toHaveLength(1);

  await user.click(await browse());
  const confirm = within(await screen.findByRole('dialog')).getByRole('button', { name: /confirm/i });
  expect(confirm).toBeEnabled();
  await user.click(confirm);
  await waitFor(() => expect(api.requests(bookingsPath, 'POST')).toHaveLength(2));
  expect(api.requests(bookingsPath, 'POST')[1].body).toEqual({ slotId: ids.otherSlot });
  await screen.findByText(/booking confirmed/i);
});

test.each([
  ['book', 401, 'UNAUTHENTICATED'],
  ['book', 403, 'ACCOUNT_INACTIVE'],
  ['cancel', 401, 'UNAUTHENTICATED'],
  ['cancel', 403, 'ACCOUNT_INACTIVE'],
] as const)('FE-R1 %s CSRF acquisition %s %s clears session before mutation and rejects late private data', async (operation, status, code) => {
  const api = apiFixture(student);
  const csrfResponse = deferred<Response>();
  const lateModules = deferred<Response>();
  api.on('GET', '/api/auth/csrf', () => csrfResponse.promise);
  api.on('GET', '/api/student/modules', () => lateModules.promise);
  if (operation === 'cancel') window.history.replaceState({}, '', '/student/bookings');
  render(<App />);
  await screen.findByText(slot.tutorName);
  expect(screen.getByText(student.name)).toBeVisible();
  // A restored actor has no cached token: the first unsafe operation must
  // acquire one, and that GET can itself report revocation/deactivation.
  expect(api.requests('/api/auth/csrf')).toHaveLength(0);
  const user = userEvent.setup();
  const mutationPath = operation === 'book' ? bookingsPath : `${bookingsPath}/${ids.booking}/cancel`;
  await user.click(screen.getByRole('button', { name: operation === 'book' ? /^book(?: consultation)?$/i : /^cancel(?: booking)?$/i }));
  await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: /confirm/i }));
  await waitFor(() => expect(api.requests('/api/auth/csrf')).toHaveLength(1));
  expect(api.requests(mutationPath, 'POST')).toHaveLength(0);
  await act(async () => csrfResponse.resolve(failure(status, code)));
  expect(api.requests(mutationPath, 'POST')).toHaveLength(0);
  await screen.findByLabelText(/^email$/i);
  expect(window.location.pathname).toBe('/login');
  expect(await screen.findByRole('alert')).toHaveTextContent(code === 'ACCOUNT_INACTIVE' ? /inactive/i : /session.*expired|sign in again/i);
  expect(screen.queryByText(student.name)).not.toBeInTheDocument();
  expect(screen.queryByText(slot.tutorName)).not.toBeInTheDocument();
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  // The test fetch deliberately ignores abort to exercise the stale-result
  // boundary in addition to normal browser cancellation.
  await act(async () => lateModules.resolve(json([{ id: ids.module, code: 'OLD-PRIVATE', name: 'Late prior session module' }])));
  expect(screen.queryByText(/OLD-PRIVATE|Late prior session module/)).not.toBeInTheDocument();
  expect(screen.queryByText(slot.tutorName)).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: /^book(?: consultation)?$|^cancel(?: booking)?$/i })).not.toBeInTheDocument();
  expect(screen.getByLabelText(/^email$/i)).toBeVisible();
  expect(api.requests(mutationPath, 'POST')).toHaveLength(0);
  expect(api.requests('/api/auth/me')).toHaveLength(1);
});
