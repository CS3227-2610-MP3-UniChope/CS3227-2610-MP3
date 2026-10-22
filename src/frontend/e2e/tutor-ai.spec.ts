import { expect, test } from '@playwright/test';

const bookingId = '40000000-0000-4000-8000-000000000010';
const tutor = { id: '90000000-0000-4000-8000-000000000001', name: 'Browser Synthetic Tutor', role: 'TUTOR' };
const booking = {
  bookingId,
  slotId: '30000000-0000-4000-8000-000000000010',
  moduleCode: 'CS3227',
  startTime: '2026-10-21T02:00:00Z',
  endTime: '2026-10-21T03:00:00Z',
  status: 'COMPLETED',
};

test('Tutor reviews a fake draft and explicitly saves the note', async ({ page }) => {
  let actor: typeof tutor | null = null;
  let bookingCompleted = false;
  const calls: Array<{ method: string; url: URL; body: unknown }> = [];
  const draft = 'Summary\nReviewed recursion.\n\nTopics discussed\nBase cases.\n\nFollow-up\nPractise examples.';

  await page.route('**/api/**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    const body = request.postDataJSON() ?? null;
    calls.push({ method, url, body });
    let status = 200;
    let response: unknown;

    if (url.pathname === '/api/auth/me') {
      if (!actor) { status = 401; response = { code: 'UNAUTHENTICATED' }; }
      else response = actor;
    } else if (url.pathname === '/api/auth/csrf') {
      response = { headerName: 'X-BROWSER-CSRF', token: 'synthetic-csrf-token' };
    } else if (url.pathname === '/api/auth/login' && method === 'POST') {
      actor = tutor; response = tutor;
    } else if (url.pathname === '/api/auth/logout' && method === 'POST') {
      actor = null; status = 204;
    } else if (url.pathname === '/api/tutor/modules' && method === 'GET') {
      response = [];
    } else if (url.pathname === '/api/tutor/bookings' && method === 'GET') {
      response = url.searchParams.get('status') === 'COMPLETED' ? [booking]
        : [{ ...booking, status: bookingCompleted ? 'COMPLETED' : 'ACTIVE' }];
    } else if (url.pathname === `/api/tutor/bookings/${bookingId}/complete` && method === 'POST') {
      bookingCompleted = true; response = { id: bookingId, status: 'COMPLETED' };
    } else if (url.pathname === '/api/tutor/history/slots' && method === 'GET') {
      response = [{ id: booking.slotId, moduleId: '20000000-0000-4000-8000-000000000001', moduleCode: booking.moduleCode,
        startTime: booking.startTime, endTime: booking.endTime, status: 'COMPLETED' }];
    } else if (url.pathname === `/api/tutor/bookings/${bookingId}/note` && method === 'GET') {
      response = { bookingId, content: null, version: 0, updatedAt: null };
    } else if (url.pathname === `/api/tutor/bookings/${bookingId}/note-draft` && method === 'POST') {
      response = { bookingId, draft, requiresReview: true, providerMode: 'fake' };
    } else if (url.pathname === `/api/tutor/bookings/${bookingId}/note` && method === 'PUT') {
      const submitted = body as { content: string; expectedVersion: number };
      expect(submitted.expectedVersion).toBe(0);
      response = { bookingId, content: submitted.content, version: 1, updatedAt: '2026-10-22T12:00:00Z' };
    } else {
      status = 404; response = { code: 'NOT_FOUND' };
    }

    await route.fulfill({ status, contentType: 'application/json', body: status === 204 ? '' : JSON.stringify(response) });
  });

  const pageErrors: string[] = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  await page.goto('/login');
  await page.getByLabel('Email').fill('tutor@example.edu');
  await page.getByLabel('Password').fill('synthetic-password');
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page).toHaveURL(/\/tutor$/);
  await expect(page.getByRole('heading', { name: 'Consultation notes' })).toBeVisible();
  await expect(page.getByLabel('Completed consultation')).toContainText('CS3227');
  await expect(page.getByLabel('Tutor-only note')).toHaveValue('');
  expect(calls.some(call => call.url.pathname === '/api/tutor/bookings' && call.url.searchParams.get('status') === 'COMPLETED')).toBe(true);

  page.on('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: 'Mark complete' }).click();
  await expect(page.getByText('Consultation marked completed.')).toBeVisible();
  expect(calls.find(call => call.url.pathname === `/api/tutor/bookings/${bookingId}/complete`)?.body).toEqual({});
  await expect(page.getByRole('cell', { name: 'COMPLETED' })).toBeVisible();
  await page.getByRole('button', { name: 'Load history' }).click();
  await expect(page.getByRole('heading', { name: 'Slot history' })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Slot history' }).getByRole('cell', { name: 'CS3227' })).toBeVisible();
  expect(calls.some(call => call.url.pathname === '/api/tutor/history/slots'
    && call.url.searchParams.get('status') === 'COMPLETED')).toBe(true);
  await expect(page.getByLabel('Completed consultation')).toContainText('CS3227');

  await page.getByLabel('Rough notes').fill('Discuss recursion base cases');
  await page.getByRole('button', { name: 'Generate draft' }).click();
  await expect(page.getByLabel('Editable draft')).toHaveValue(draft);
  await expect(page.getByText('FAKE RESULT')).toBeVisible();
  expect(calls.filter(call => call.method === 'PUT')).toHaveLength(0);

  await page.getByLabel('Editable draft').fill('Reviewed and corrected summary');
  await page.getByRole('button', { name: 'Use reviewed draft' }).click();
  await expect(page.getByLabel('Tutor-only note')).toHaveValue('Reviewed and corrected summary');
  expect(calls.filter(call => call.method === 'PUT')).toHaveLength(0);

  await page.getByRole('button', { name: 'Save note' }).click();
  await expect(page.getByText('Note saved.')).toBeVisible();
  const saves = calls.filter(call => call.method === 'PUT');
  expect(saves).toHaveLength(1);
  expect(saves[0].body).toEqual({ content: 'Reviewed and corrected summary', expectedVersion: 0 });
  expect(pageErrors).toEqual([]);
});
