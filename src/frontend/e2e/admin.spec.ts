import { expect, test, type Page } from '@playwright/test';

const statistics = { capturedAt: '2026-10-23T01:00:00Z', total: 4, completed: 2, cancelled: 1, completionRate: 50, cancellationRate: 25, byModule: [{ id: 'm1', label: 'CS3227', count: 4 }], byTutor: [] };
const draft = '<script>untrusted()</script> Aggregate booking summary.';
async function adminApi(page: Page) {
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname;
    const bodies: Record<string, unknown> = {
      '/api/auth/me': { id: 'a1', name: 'Synthetic Admin', role: 'ADMIN' },
      '/api/auth/csrf': { headerName: 'X-TEST-CSRF', token: 'test-only' },
      '/api/admin/users': [{ id: 't1', name: 'Synthetic Tutor', email: 'tutor@example.edu', role: 'TUTOR', active: true }],
      '/api/admin/modules': [{ id: 'm1', code: 'CS3227', name: 'Software Engineering', active: true }],
      '/api/admin/assignments': [{ tutorId: 't1', moduleId: 'm1' }],
      '/api/admin/bookings': [],
      '/api/admin/statistics': statistics,
      '/api/admin/activity-report': { capturedAt: statistics.capturedAt, statistics, draft, requiresReview: true, providerMode: 'fake' },
    };
    await route.fulfill({ status: path in bodies ? 200 : 404, contentType: 'application/json', body: JSON.stringify(bodies[path] ?? { code: 'NOT_FOUND' }) });
  });
}

for (const width of [320, 768, 1440]) {
  test(`Admin sections remain usable at ${width}px without page overflow`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await adminApi(page);
    await page.goto('/admin');
    await expect(page.getByRole('heading', { name: 'Admin overview', exact: true })).toBeVisible();
    const navigation = page.getByRole('navigation', { name: 'Admin navigation' });
    for (const label of ['Users', 'Modules', 'Tutor assignments', 'Booking activity', 'Activity report assistant', 'Overview']) {
      await navigation.getByRole('link', { name: label, exact: true }).click();
      await expect(page.getByRole('heading', { name: label === 'Overview' ? 'Admin overview' : label, exact: true, level: 1 })).toBeVisible();
      expect(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1)).toBe(false);
    }
    await page.getByRole('button', { name: 'Dark theme', exact: true }).click();
    expect(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1)).toBe(false);
  });
}

test('Admin confirmation traps keyboard focus and restores it on Escape', async ({ page }) => {
  await adminApi(page); await page.goto('/admin/users');
  const trigger = page.getByRole('button', { name: 'Deactivate', exact: true });
  await trigger.click();
  const dialog = page.getByRole('dialog');
  const back = dialog.getByRole('button', { name: 'Go back' });
  const confirm = dialog.getByRole('button', { name: 'Confirm change' });
  await expect(back).toBeFocused();
  await page.keyboard.press('Shift+Tab'); await expect(confirm).toBeFocused();
  await page.keyboard.press('Tab'); await expect(back).toBeFocused();
  await page.keyboard.press('Escape'); await expect(dialog).not.toBeVisible();
  await expect(trigger).toBeFocused();
});

test('Activity report is an explicit CSRF protected request and renders mock text with its snapshot', async ({ page }) => {
  let reports = 0;
  await adminApi(page);
  page.on('request', request => {
    if (new URL(request.url()).pathname === '/api/admin/activity-report') {
      reports++;
      expect(request.method()).toBe('POST');
      expect(request.postDataJSON()).toEqual({});
      expect(request.headers()['x-test-csrf']).toBe('test-only');
    }
  });
  await page.goto('/admin/reports');
  await expect(page.getByRole('heading', { name: 'Activity report assistant', level: 1 })).toBeVisible();
  expect(reports).toBe(0);
  await page.getByRole('button', { name: 'Generate activity report' }).click();
  await expect(page.getByText(/Mock AI response/)).toBeVisible();
  await expect(page.getByText(draft, { exact: true })).toBeVisible();
  await expect(page.locator('.admin-report script')).toHaveCount(0);
  await expect(page.getByText('50%', { exact: true })).toBeVisible();
  await expect(page.getByText('25%', { exact: true })).toBeVisible();
  expect(reports).toBe(1);
  await page.getByRole('navigation', { name: 'Admin navigation' }).getByRole('link', { name: 'Overview', exact: true }).click();
  await page.getByRole('navigation', { name: 'Admin navigation' }).getByRole('link', { name: 'Activity report assistant', exact: true }).click();
  await expect(page.getByText(draft, { exact: true })).toHaveCount(0);
  expect(reports).toBe(1);
});
