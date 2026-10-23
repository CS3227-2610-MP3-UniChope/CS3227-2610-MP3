import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import App from '../App';
import { apiFixture, booking, deferred, ids, json, student } from './api-fixture';

const detail = `/api/student/bookings/${ids.booking}`;
const endpoint = `${detail}/agenda`;
const route = `/student/bookings/${ids.booking}/agenda`;
const future = { ...booking, startTime: '2099-10-21T02:00:00Z', endTime: '2099-10-21T03:00:00Z' };
const result = (agenda: string) => ({ bookingId: ids.booking, agenda, requiresReview: true, providerMode: 'fake' });
beforeEach(() => { window.history.replaceState({}, '', route); });
afterEach(() => { vi.unstubAllGlobals(); });
function setup() {
  const api = apiFixture(student);
  api.givenBookings([future]); api.on('GET', detail, () => json(future));
  return api;
}

test('AG-R1 S-UI-1 regeneration cannot overwrite an edit accepted while its response is pending', async () => {
  const api = setup(); const next = deferred<Response>();
  let attempts = 0;
  api.on('POST', endpoint, () => ++attempts === 1 ? json(result('First generated agenda')) : next.promise);
  const user = userEvent.setup(); render(<App />);
  await user.type(await screen.findByRole('textbox', { name: /questions|topics/i }), 'Synthetic recursion question');
  await user.click(screen.getByRole('button', { name: /^generate agenda$/i }));
  const editor = await screen.findByRole('textbox', { name: /draft|agenda/i });
  expect(editor).toHaveValue('First generated agenda');
  await user.click(screen.getByRole('button', { name: /regenerate agenda/i }));
  await waitFor(() => expect(api.requests(endpoint, 'POST')).toHaveLength(2));

  // Either prevent editing while pending, or protect any edit the UI accepts.
  const writable = !(editor as HTMLTextAreaElement).disabled && !(editor as HTMLTextAreaElement).readOnly;
  if (writable) {
    await user.clear(editor); await user.type(editor, 'My edit made while regeneration was pending');
    expect(editor).toHaveValue('My edit made while regeneration was pending');
  }
  await act(async () => { next.resolve(json(result('Second generated agenda'))); });
  if (writable) {
    expect(editor).toHaveValue('My edit made while regeneration was pending');
    // Discarding a stale result is also valid. If replacement is offered, declining is safe.
    const confirmation = screen.queryByRole('dialog');
    if (confirmation) {
      await user.keyboard('{Escape}');
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(editor).toHaveValue('My edit made while regeneration was pending');
    }
  } else {
    await waitFor(() => expect(editor).toHaveValue('Second generated agenda'));
  }
  expect(api.requests(endpoint, 'POST')).toHaveLength(2);
});

test('AG-R2 S-AGENDA-PRIVACY browser Back warns before discarding questions; Escape retains them', async () => {
  const api = setup(); const user = userEvent.setup();
  window.history.replaceState({}, '', '/student/bookings'); render(<App />);
  await user.click(await screen.findByRole('link', { name: /prepare agenda/i }));
  const q = await screen.findByRole('textbox', { name: /questions|topics/i });
  await user.type(q, 'Unsaved synthetic preparation questions');
  expect(window.location.pathname).toBe(route);
  act(() => { window.history.back(); });
  const confirmation = await screen.findByRole('dialog');
  expect(confirmation).toHaveAttribute('aria-modal', 'true');
  await user.keyboard('{Escape}');
  await waitFor(() => expect(window.location.pathname).toBe(route));
  expect(await screen.findByRole('textbox', { name: /questions|topics/i })).toHaveValue('Unsaved synthetic preparation questions');
  expect(api.requests(endpoint, 'POST')).toHaveLength(0);

  act(() => { window.history.back(); });
  const leave = await screen.findByRole('dialog');
  await user.click(within(leave).getByRole('button', { name: /leave|discard|confirm/i }));
  await waitFor(() => expect(window.location.pathname).toBe('/student/bookings'));
  expect(screen.queryByDisplayValue('Unsaved synthetic preparation questions')).not.toBeInTheDocument();
});
