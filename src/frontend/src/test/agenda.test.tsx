import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import App from '../App';
import { apiFixture, booking, deferred, failure, ids, json, student } from './api-fixture';

const detail = `/api/student/bookings/${ids.booking}`;
const endpoint = `${detail}/agenda`;
const route = `/student/bookings/${ids.booking}/agenda`;
const future = { ...booking, startTime: '2099-10-21T02:00:00Z', endTime: '2099-10-21T03:00:00Z' };
const draft = 'Goal\nClarify recursion\n\nAgenda (60 min)\n1. Base cases (10 min)\n   How do base cases work?';
const result = { bookingId: ids.booking, agenda: draft, requiresReview: true, providerMode: 'fake' };
beforeEach(() => { window.history.replaceState({}, '', route); });
afterEach(() => { vi.unstubAllGlobals(); });
function setup() {
  const api = apiFixture(student);
  api.givenBookings([future]);
  api.on('GET', detail, () => json(future));
  api.on('POST', endpoint, () => json(result));
  return api;
}
async function questions() { return screen.findByRole('textbox', { name: /questions|topics/i }); }
function generate() { return screen.getByRole('button', { name: /generate|regenerate/i }); }
async function generated(user: ReturnType<typeof userEvent.setup>) {
  await user.type(await questions(), 'How do base cases work?');
  await user.click(generate());
  return screen.findByRole('textbox', { name: /draft|agenda/i });
}

test('S-AGENDA-ROUTE direct URL restores owned summary and has privacy reminder, questions count and no early POST', async () => {
  const api = setup(); render(<App />);
  expect(await questions()).toBeVisible();
  expect(window.location.pathname).toBe(route);
  expect(document.body).toHaveTextContent(/CS3227/);
  expect(document.body).toHaveTextContent(/SGT/);
  expect(document.body).toHaveTextContent(/personal|sensitive/i);
  expect(document.body).toHaveTextContent(/4[,.]?000/);
  expect(api.requests(endpoint, 'POST')).toHaveLength(0);
});

test('S-AI-1 EDIT generate sends only questions with CSRF; editable plain text, fake/review labels and clipboard', async () => {
  const api = setup(); const user = userEvent.setup();
  render(<App />);
  const editor = await generated(user);
  expect(editor).toHaveValue(draft);
  expect(document.body).toHaveTextContent(/AI.generated/i);
  expect(document.body).toHaveTextContent(/fake/i);
  expect(document.body).toHaveTextContent(/review/i);
  const sent = api.requests(endpoint, 'POST'); expect(sent).toHaveLength(1);
  expect(sent[0].body).toEqual({ questions: 'How do base cases work?' });
  expect(sent[0].headers.get('X-TEST-CSRF')).toBeTruthy();
  await user.clear(editor); await user.type(editor, '<script>synthetic()</script> edited draft');
  expect(document.querySelector('script')).toBeNull();
  await user.click(screen.getByRole('button', { name: /copy/i }));
  expect(document.body).toHaveTextContent(/copied|clipboard|select.*copy|copy.*manually/i);
  expect(api.calls.filter(c => c.method !== 'GET').every(c => c.url.pathname === endpoint)).toBe(true);
  expect(JSON.stringify(localStorage)).not.toContain('synthetic');
  expect(JSON.stringify(sessionStorage)).not.toContain('synthetic');
});

test('S-AGENDA-INPUT Unicode count accepts 4000 astral points, rejects 4001 and Unicode whitespace without calls', async () => {
  const api = setup(); const user = userEvent.setup(); render(<App />);
  const q = await questions();
  await user.click(q); await user.paste('\u2003'); await user.click(generate());
  expect(api.requests(endpoint, 'POST')).toHaveLength(0);
  await user.clear(q); await user.click(q); await user.paste('😀'.repeat(4000));
  expect(q).toHaveValue('😀'.repeat(4000));
  await user.click(generate()); await screen.findByRole('textbox', { name: /draft|agenda/i });
  expect(api.requests(endpoint, 'POST')).toHaveLength(1);
  await user.clear(q); await user.click(q); await user.paste('😀'.repeat(4001)); await user.click(generate());
  expect(api.requests(endpoint, 'POST')).toHaveLength(1);
});

test('S-UI-1 edited draft requires accessible regenerate confirmation; Escape preserves it with no call', async () => {
  const api = setup(); const user = userEvent.setup(); render(<App />);
  const editor = await generated(user); await user.type(editor, ' my edit');
  await user.click(generate()); const dialog = await screen.findByRole('dialog');
  expect(dialog).toHaveAttribute('aria-modal', 'true');
  await user.keyboard('{Escape}');
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(editor).toHaveValue(`${draft} my edit`); expect(api.requests(endpoint, 'POST')).toHaveLength(1);
  await user.click(generate());
  await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: /confirm|replace|regenerate/i }));
  await waitFor(() => expect(api.requests(endpoint, 'POST')).toHaveLength(2));
});

test.each([[504, 'AI_TIMEOUT'], [502, 'AI_INVALID_OUTPUT'], [503, 'AI_UNAVAILABLE'], [409, 'AI_BUSY'], [429, 'AI_RATE_LIMITED']] as const)(
  'S-AI-6 FAILURE %s preserves questions and previous draft, announces retry and never auto-retries', async (status, code) => {
    const api = setup(); const user = userEvent.setup(); render(<App />);
    const editor = await generated(user);
    api.on('POST', endpoint, () => failure(status, code, { 'Retry-After': '37' }));
    await user.click(generate());
    // An unedited generated draft requires no overwrite confirmation.
    const alert = await screen.findByRole('alert'); expect(alert).toBeVisible();
    if (status === 409 || status === 429) expect(alert).toHaveTextContent(/37/);
    expect(editor).toHaveValue(draft);
    expect(await questions()).toHaveValue('How do base cases work?');
    expect(api.requests(endpoint, 'POST')).toHaveLength(2); expect(generate()).toBeEnabled();
    expect(screen.getByRole('link', { name: /bookings/i })).toBeVisible();
  });

test('S-AGENDA-RACE pending progress prevents concurrent clicks and sign-out discards late completion, clears private state', async () => {
  const api = setup(); const pending = deferred<Response>(); api.on('POST', endpoint, () => pending.promise);
  const user = userEvent.setup(); render(<App />);
  await user.type(await questions(), 'private synthetic questions'); await user.click(generate());
  expect(generate()).toBeDisabled(); expect(api.requests(endpoint, 'POST')).toHaveLength(1);
  await user.click(screen.getByRole('button', { name: /sign out/i }));
  await screen.findByRole('textbox', { name: /email/i });
  await act(async () => { pending.resolve(json(result)); });
  expect(document.body).not.toHaveTextContent('Clarify recursion');
  expect(screen.queryByDisplayValue('private synthetic questions')).not.toBeInTheDocument();
});

test('S-AGENDA-PRIVACY dirty page warns on in-app departure and unload; decline keeps questions', async () => {
  setup(); const user = userEvent.setup(); render(<App />);
  await user.type(await questions(), 'synthetic private questions');
  const unload = new Event('beforeunload', { cancelable: true }); window.dispatchEvent(unload);
  expect(unload.defaultPrevented).toBe(true);
  await user.click(screen.getByRole('link', { name: /my bookings/i }));
  await screen.findByRole('dialog'); await user.keyboard('{Escape}');
  expect(window.location.pathname).toBe(route);
  expect(await questions()).toHaveValue('synthetic private questions');
});

test('S-AGENDA-RACE route switch discards pending response without showing previous booking agenda', async () => {
  const api = setup(); const pending = deferred<Response>(); api.on('POST', endpoint, () => pending.promise);
  const otherId = '40000000-0000-4000-8000-000000000002';
  api.on('GET', `/api/student/bookings/${otherId}`, () => json({ ...future, id: otherId, moduleCode: 'CS9999' }));
  const user = userEvent.setup(); render(<App />);
  await user.type(await questions(), 'first booking questions'); await user.click(generate());
  await act(async () => { window.history.pushState({}, '', `/student/bookings/${otherId}/agenda`); window.dispatchEvent(new PopStateEvent('popstate')); });
  await act(async () => { pending.resolve(json(result)); });
  expect(document.body).not.toHaveTextContent('Clarify recursion');
  expect(screen.queryByDisplayValue('first booking questions')).not.toBeInTheDocument();
});

test.each([401, 403, 404, 409])('S-AGENDA-ROUTE detail denial %s has safe feedback and no generation action', async status => {
  const api = setup(); api.on('GET', detail, () => failure(status, status === 401 ? 'UNAUTHENTICATED' : status === 403 ? 'ACCOUNT_INACTIVE' : status === 404 ? 'NOT_FOUND' : 'INVALID_STATE'));
  render(<App />); await screen.findByRole('alert');
  expect(screen.queryByRole('button', { name: /generate agenda/i })).not.toBeInTheDocument();
  expect(api.requests(endpoint, 'POST')).toHaveLength(0);
});

test('S-AGENDA-PRIVACY session expiry clears existing questions and draft', async () => {
  const api = setup(); const user = userEvent.setup(); render(<App />);
  await generated(user); api.on('POST', endpoint, () => failure(401, 'UNAUTHENTICATED'));
  await user.click(generate()); await screen.findByRole('textbox', { name: /email/i });
  expect(screen.queryByDisplayValue(draft)).not.toBeInTheDocument();
  expect(screen.queryByDisplayValue('How do base cases work?')).not.toBeInTheDocument();
});

test('S-AGENDA-ROUTE active unended booking exposes preparation action', async () => {
  const api = setup(); window.history.replaceState({}, '', '/student/bookings'); render(<App />);
  const action = await screen.findByRole('link', { name: /prepare agenda/i });
  expect(action).toHaveAttribute('href', route);
  const user = userEvent.setup(); await user.click(action); expect(await questions()).toBeVisible();
  expect(api.requests(endpoint, 'POST')).toHaveLength(0);
});
