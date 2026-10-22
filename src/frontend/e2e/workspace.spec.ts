import { expect, test, type Page } from '@playwright/test';

const moduleId = '20000000-0000-4000-8000-000000000001';
const slots = [
  { id: '30000000-0000-4000-8000-000000000001', tutorName: 'Early overlapping tutor', startTime: '2026-10-18T16:05:00Z', endTime: '2026-10-18T17:00:00Z' },
  { id: '30000000-0000-4000-8000-000000000002', tutorName: 'Second overlapping tutor', startTime: '2026-10-18T16:05:00Z', endTime: '2026-10-18T17:30:00Z' },
  { id: '30000000-0000-4000-8000-000000000003', tutorName: 'Late Sunday tutor with a deliberately very long synthetic display name for wrapping', startTime: '2026-10-25T15:30:00Z', endTime: '2026-10-25T16:30:00Z' },
].map(slot => ({ ...slot, moduleId, moduleCode: 'CS3227', moduleName: 'Very long synthetic module title to exercise responsive wrapping without clipping' }));

// Browser fixtures are test-only and installed before App loads. No runtime demo mode.
async function mockApi(page: Page, authenticated = true) {
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname;
    let status = 200;
    let body: unknown;
    if (path === '/api/auth/me') {
      status = authenticated ? 200 : 401;
      body = authenticated
        ? { id: '10000000-0000-4000-8000-000000000001', name: 'Browser Synthetic Student', role: 'STUDENT' }
        : { code: 'UNAUTHENTICATED', message: 'Sign in required', requestId: 'browser-test' };
    } else if (path === '/api/auth/csrf') body = { headerName: 'X-BROWSER-CSRF', token: 'test-only-token' };
    else if (path === '/api/student/modules') body = [{ id: moduleId, code: 'CS3227', name: slots[0].moduleName }];
    else if (path === '/api/student/slots') body = slots;
    else if (path === '/api/student/bookings') body = [];
    else { status = 404; body = { code: 'NOT_FOUND', message: 'No test route', requestId: 'browser-test' }; }
    await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
  });
}

for (const width of [320, 768, 1024, 1440]) {
  test(`FE2-UI responsive ${width}px all choices visible, no page/dialog horizontal overflow`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await mockApi(page);
    const errors: string[] = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.goto('/student/slots?week=2026-10-19');
    const book = page.getByRole('button', { name: /^book(?: consultation)?$/i });
    await expect(book).toHaveCount(3);
    await expect(page.getByRole('heading', { level: 1 })).toHaveCount(1);
    for (const slot of slots) await expect(page.getByText(slot.tutorName, { exact: true })).toBeVisible();
    const overflow = () => page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
    expect(await overflow()).toBe(false);
    if (width >= 1024) {
      for (const day of ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']) {
        await expect(page.getByText(new RegExp(`^${day}$|^${day.slice(0, 3)}$`, 'i')).first()).toBeVisible();
      }
      const filters = await page.getByRole('combobox', { name: /module/i }).boundingBox();
      const firstEvent = await book.first().boundingBox();
      expect(filters!.x + filters!.width).toBeLessThan(firstEvent!.x);
    } else if (width === 320) {
      const filters = await page.getByRole('combobox', { name: /module/i }).boundingBox();
      const firstEvent = await book.first().boundingBox();
      expect(filters!.y).toBeLessThan(firstEvent!.y);
    }
    await book.first().click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog).toHaveAccessibleName(/.+/);
    expect(await overflow()).toBe(false);
    const box = await dialog.boundingBox();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.x + box!.width).toBeLessThanOrEqual(width + 1);
    await expect(dialog).toContainText('SGT');
    await page.keyboard.press('Escape');
    await expect(dialog).not.toBeVisible();
    await expect(book.first()).toBeFocused();
    expect(errors).toEqual([]);
  });
}

test('FE2-UI confirmation traps Tab and Shift-Tab then restores focus on Escape', async ({ page }) => {
  await mockApi(page);
  await page.goto('/student/slots?week=2026-10-19');
  const trigger = page.getByRole('button', { name: /^book(?: consultation)?$/i }).first();
  await trigger.focus();
  await page.keyboard.press('Enter');
  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  for (const key of ['Tab', 'Tab', 'Tab', 'Shift+Tab', 'Shift+Tab', 'Shift+Tab']) {
    await page.keyboard.press(key);
    expect(await dialog.evaluate(element => element.contains(document.activeElement))).toBe(true);
  }
  await page.keyboard.press('Escape');
  await expect(trigger).toBeFocused();
});

test('FE1-UI mobile Login labels, keyboard flow, one h1 and no overflow', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 800 });
  await mockApi(page, false);
  await page.goto('/login');
  const email = page.getByLabel(/^email$/i);
  await expect(email).toBeVisible();
  await expect(page.getByRole('heading', { level: 1 })).toHaveCount(1);
  await expect(page.getByLabel(/^password$/i)).toHaveAttribute('autocomplete', 'current-password');
  await email.focus();
  await page.keyboard.press('Tab');
  await expect(page.getByLabel(/^password$/i)).toBeFocused();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1)).toBe(true);
});
