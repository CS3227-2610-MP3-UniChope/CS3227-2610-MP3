import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiClient } from '../api';
import TutorWorkspace from '../tutor/TutorWorkspace';
import { apiFixture, deferred, json } from './api-fixture';

const tutor = { id: '90000000-0000-4000-8000-000000000001', name: 'Synthetic Tutor', role: 'TUTOR' as const };
const bookingId = '40000000-0000-4000-8000-000000000010';
const anotherBookingId = '40000000-0000-4000-8000-000000000011';
const completed = [
  { bookingId, slotId: '30000000-0000-4000-8000-000000000010', moduleCode: 'CS3227', startTime: '2026-10-21T02:00:00Z', endTime: '2026-10-21T03:00:00Z' },
  { bookingId: anotherBookingId, slotId: '30000000-0000-4000-8000-000000000011', moduleCode: 'CS2103', startTime: '2026-10-20T02:00:00Z', endTime: '2026-10-20T03:00:00Z' },
];
const blankNote = (id = bookingId) => ({ bookingId: id, content: null, version: 0, updatedAt: null });
let api: ReturnType<typeof apiFixture>;

beforeEach(() => {
  api = apiFixture(tutor);
  api.on('GET', '/api/tutor/bookings', () => json(completed));
  api.on('GET', `/api/tutor/bookings/${bookingId}/note`, () => json(blankNote()));
  api.on('GET', `/api/tutor/bookings/${anotherBookingId}/note`, () => json(blankNote(anotherBookingId)));
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });

test('TUT-AI-UI generated draft stays separate until explicit copy and versioned save', async () => {
  const user = userEvent.setup();
  const draft = 'Summary\nReviewed recursion.\n\nTopics discussed\nBase cases.\n\nFollow-up\nPractise examples.';
  api.on('POST', `/api/tutor/bookings/${bookingId}/note-draft`, () => json({ bookingId, draft, requiresReview: true, providerMode: 'fake' }));
  api.on('PUT', `/api/tutor/bookings/${bookingId}/note`, call => json({ bookingId, content: (call.body as { content: string }).content, version: 1, updatedAt: '2026-10-22T12:00:00Z' }));

  render(<TutorWorkspace api={new ApiClient()} actor={tutor} />);
  await screen.findByRole('option', { name: /CS3227/ });
  expect(api.requests('/api/tutor/bookings', 'GET')[0].url.searchParams.get('status')).toBe('COMPLETED');
  await screen.findByLabelText('Tutor-only note');
  await user.type(screen.getByLabelText('Rough notes'), 'Discuss recursion base cases');
  await user.click(screen.getByRole('button', { name: 'Generate draft' }));
  const editableDraft = await screen.findByLabelText('Editable draft');
  expect(editableDraft).toHaveValue(draft);
  expect(screen.getByText('FAKE RESULT')).toBeVisible();
  expect(api.requests(`/api/tutor/bookings/${bookingId}/note`, 'PUT')).toHaveLength(0);
  expect(api.requests(`/api/tutor/bookings/${bookingId}/note-draft`, 'POST')[0].headers.get('X-TEST-CSRF')).toBeTruthy();

  await user.clear(editableDraft);
  await user.type(editableDraft, 'Reviewed and corrected summary');
  await user.click(screen.getByRole('button', { name: 'Use reviewed draft' }));
  expect(screen.getByLabelText('Tutor-only note')).toHaveValue('Reviewed and corrected summary');
  expect(api.requests(`/api/tutor/bookings/${bookingId}/note`, 'PUT')).toHaveLength(0);
  await user.click(screen.getByRole('button', { name: 'Save note' }));
  await waitFor(() => expect(api.requests(`/api/tutor/bookings/${bookingId}/note`, 'PUT')).toHaveLength(1));
  expect(api.requests(`/api/tutor/bookings/${bookingId}/note`, 'PUT')[0].body).toEqual({
    content: 'Reviewed and corrected summary', expectedVersion: 0,
  });
});

test('TUT-AI-UI asks before replacing a cleared but dirty saved note', async () => {
  const user = userEvent.setup();
  api.on('GET', `/api/tutor/bookings/${bookingId}/note`, () => json({
    bookingId, content: 'Previously saved note', version: 3, updatedAt: '2026-10-21T04:00:00Z',
  }));
  api.on('POST', `/api/tutor/bookings/${bookingId}/note-draft`, () => json({
    bookingId, draft: 'Reviewed draft', requiresReview: true, providerMode: 'fake',
  }));
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);

  render(<TutorWorkspace api={new ApiClient()} actor={tutor} />);
  const noteEditor = await screen.findByLabelText('Tutor-only note');
  await waitFor(() => expect(noteEditor).toHaveValue('Previously saved note'));
  await user.clear(noteEditor);
  await user.type(screen.getByLabelText('Rough notes'), 'Reviewed recursion');
  await user.click(screen.getByRole('button', { name: 'Generate draft' }));
  await screen.findByLabelText('Editable draft');
  await user.click(screen.getByRole('button', { name: 'Use reviewed draft' }));

  expect(confirm).toHaveBeenCalledWith('Replace the text in the note editor with this reviewed draft?');
  expect(noteEditor).toHaveValue('');
  expect(api.requests(`/api/tutor/bookings/${bookingId}/note`, 'PUT')).toHaveLength(0);
});

test('TUT-AI-UI preserves buffers and original version after a note conflict', async () => {
  const user = userEvent.setup();
  api.on('GET', `/api/tutor/bookings/${bookingId}/note`, () => json({
    bookingId, content: 'Existing note', version: 4, updatedAt: '2026-10-21T04:00:00Z',
  }));
  api.on('POST', `/api/tutor/bookings/${bookingId}/note-draft`, () => json({
    bookingId, draft: 'Reviewed draft', requiresReview: true, providerMode: 'fake',
  }));
  api.on('PUT', `/api/tutor/bookings/${bookingId}/note`, () => new Response(
    JSON.stringify({ code: 'NOTE_CONFLICT', message: 'safe' }),
    { status: 409, headers: { 'Content-Type': 'application/json' } },
  ));
  vi.spyOn(window, 'confirm').mockReturnValue(true);

  render(<TutorWorkspace api={new ApiClient()} actor={tutor} />);
  const noteEditor = await screen.findByLabelText('Tutor-only note');
  await waitFor(() => expect(noteEditor).toHaveValue('Existing note'));
  await user.type(screen.getByLabelText('Rough notes'), 'Keep this rough input');
  await user.click(screen.getByRole('button', { name: 'Generate draft' }));
  await screen.findByLabelText('Editable draft');
  await user.click(screen.getByRole('button', { name: 'Use reviewed draft' }));
  await user.click(screen.getByRole('button', { name: 'Save note' }));

  await screen.findByRole('alert');
  expect(api.requests(`/api/tutor/bookings/${bookingId}/note`, 'PUT')).toHaveLength(1);
  expect(api.requests(`/api/tutor/bookings/${bookingId}/note`, 'PUT')[0].body).toEqual({
    content: 'Reviewed draft', expectedVersion: 4,
  });
  expect(api.requests(`/api/tutor/bookings/${bookingId}/note`, 'GET')).toHaveLength(1);
  expect(noteEditor).toHaveValue('Reviewed draft');
  expect(screen.getByLabelText('Rough notes')).toHaveValue('Keep this rough input');
  expect(screen.getByLabelText('Editable draft')).toHaveValue('Reviewed draft');
  expect(screen.getByText('This saved note changed elsewhere. Reload it before saving your edits.')).toBeVisible();
});

test('TUT-AI-UI preserves rough input on provider failure and asks before replacing edited draft', async () => {
  const user = userEvent.setup();
  let count = 0;
  api.on('POST', `/api/tutor/bookings/${bookingId}/note-draft`, () => {
    count++;
    return count === 1 ? json({ bookingId, draft: 'First draft', requiresReview: true, providerMode: 'fake' })
      : new Response(JSON.stringify({ code: 'AI_TIMEOUT', message: 'safe' }), { status: 504, headers: { 'Content-Type': 'application/json' } });
  });
  vi.spyOn(window, 'confirm').mockReturnValue(true);
  render(<TutorWorkspace api={new ApiClient()} actor={tutor} />);
  await screen.findByLabelText('Tutor-only note');
  await user.type(screen.getByLabelText('Rough notes'), 'Keep these rough notes');
  await user.click(screen.getByRole('button', { name: 'Generate draft' }));
  const draftEditor = await screen.findByLabelText('Editable draft');
  await user.clear(draftEditor); await user.type(draftEditor, 'My edited draft');
  await user.click(screen.getByRole('button', { name: 'Generate again' }));
  expect(window.confirm).toHaveBeenCalledWith('Replace your edited draft with a new generated draft?');
  await screen.findByRole('alert');
  expect(api.requests(`/api/tutor/bookings/${bookingId}/note-draft`, 'POST')).toHaveLength(2);
  expect(screen.getByLabelText('Rough notes')).toHaveValue('Keep these rough notes');
  expect(screen.getByLabelText('Editable draft')).toHaveValue('My edited draft');
  expect(screen.getByText('Drafting took too long. Your notes are still here; you can continue manually.')).toBeVisible();
});

test('TUT-AI-UI preserves all note buffers for admission and provider failures', async () => {
  const user = userEvent.setup();
  const failures = [
    { code: 'AI_BUSY', status: 409, message: 'Another draft is already being prepared. Wait a moment and try again.' },
    { code: 'AI_RATE_LIMITED', status: 429, message: 'The draft limit has been reached. Try again later.' },
    { code: 'AI_UNAVAILABLE', status: 503, message: 'Drafting is temporarily unavailable. Your notes are still here.' },
    { code: 'AI_INVALID_OUTPUT', status: 502, message: 'The draft could not be checked safely. Your notes are still here.' },
  ];
  let count = 0;
  api.on('POST', `/api/tutor/bookings/${bookingId}/note-draft`, () => {
    if (count++ === 0) return json({ bookingId, draft: 'Initial draft', requiresReview: true, providerMode: 'fake' });
    const failure = failures[count - 2];
    return new Response(JSON.stringify({ code: failure.code, message: 'safe' }), {
      status: failure.status, headers: { 'Content-Type': 'application/json' },
    });
  });
  vi.spyOn(window, 'confirm').mockReturnValue(true);

  render(<TutorWorkspace api={new ApiClient()} actor={tutor} />);
  const noteEditor = await screen.findByLabelText('Tutor-only note');
  const roughEditor = screen.getByLabelText('Rough notes');
  await user.type(roughEditor, 'Keep this rough input');
  await user.click(screen.getByRole('button', { name: 'Generate draft' }));
  const draftEditor = await screen.findByLabelText('Editable draft');
  await user.clear(draftEditor);
  await user.type(draftEditor, 'Tutor revised draft');

  for (const failure of failures) {
    await user.click(screen.getByRole('button', { name: 'Generate again' }));
    expect(await screen.findByText(failure.message)).toBeVisible();
    expect(roughEditor).toHaveValue('Keep this rough input');
    expect(draftEditor).toHaveValue('Tutor revised draft');
    expect(noteEditor).toHaveValue('');
  }
  expect(api.requests(`/api/tutor/bookings/${bookingId}/note-draft`, 'POST')).toHaveLength(5);
  expect(api.requests(`/api/tutor/bookings/${bookingId}/note`, 'PUT')).toHaveLength(0);
});

test('TUT-AI-UI selection change confirms before discarding rough notes and draft', async () => {
  const user = userEvent.setup();
  api.on('POST', `/api/tutor/bookings/${bookingId}/note-draft`, () => json({ bookingId, draft: 'Draft', requiresReview: true, providerMode: 'fake' }));
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
  render(<TutorWorkspace api={new ApiClient()} actor={tutor} />);
  await screen.findByLabelText('Tutor-only note');
  await user.type(screen.getByLabelText('Rough notes'), 'Private rough notes');
  const leave = new Event('beforeunload', { cancelable: true });
  window.dispatchEvent(leave);
  expect(leave.defaultPrevented).toBe(true);
  await user.click(screen.getByLabelText('Completed consultation'));
  await user.selectOptions(screen.getByLabelText('Completed consultation'), anotherBookingId);
  expect(confirm).toHaveBeenCalled();
  expect(screen.getByLabelText('Completed consultation')).toHaveValue(bookingId);
  expect(screen.getByLabelText('Rough notes')).toHaveValue('Private rough notes');
});

test('TUT-AI-UI late generation response cannot appear under a newly selected consultation', async () => {
  const user = userEvent.setup();
  const late = deferred<Response>();
  api.on('POST', `/api/tutor/bookings/${bookingId}/note-draft`, () => late.promise);
  vi.spyOn(window, 'confirm').mockReturnValue(true);
  render(<TutorWorkspace api={new ApiClient()} actor={tutor} />);
  await screen.findByLabelText('Tutor-only note');
  await user.type(screen.getByLabelText('Rough notes'), 'First booking rough notes');
  await user.click(screen.getByRole('button', { name: 'Generate draft' }));
  await waitFor(() => expect(api.requests(`/api/tutor/bookings/${bookingId}/note-draft`, 'POST')).toHaveLength(1));
  await user.selectOptions(screen.getByLabelText('Completed consultation'), anotherBookingId);
  await waitFor(() => expect(screen.getByLabelText('Tutor-only note')).toBeEnabled());
  await act(async () => late.resolve(json({ bookingId, draft: 'Late first-booking draft', requiresReview: true, providerMode: 'fake' })));
  expect(screen.getByLabelText('Completed consultation')).toHaveValue(anotherBookingId);
  expect(screen.queryByLabelText('Editable draft')).not.toBeInTheDocument();
  expect(screen.getByLabelText('Rough notes')).toHaveValue('');
});

test('TUT-AI-UI counts Unicode code points for the rough-note limit', async () => {
  render(<TutorWorkspace api={new ApiClient()} actor={tutor} />);
  await screen.findByLabelText('Rough notes');
  const input = screen.getByLabelText('Rough notes');
  fireEvent.change(input, { target: { value: '😀'.repeat(8001) } });
  expect(screen.getByRole('button', { name: 'Generate draft' })).toBeDisabled();
  expect(screen.getByText('8001 / 8000 characters')).toBeVisible();
});
