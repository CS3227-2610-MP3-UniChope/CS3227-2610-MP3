import { expect, test } from '@playwright/test';
import { audit } from './contrast-audit';

import { fixture } from './theme-fixture';
const slot = { tutorName: 'Contrast Synthetic Tutor' };
for (const theme of ['light','dark'] as const) for (const [state, width] of [['login',1440],['login-error',1440],['slots',1440],['slots',320],['bookings',1440],['history',1440],['bookings-empty',1440],['dialog',1440],['success',1440],['error',1440],['status',1440],['loading',1440],['tutor',1440],['admin',1440],['failure',1440],['notfound',1440]] as const) {
  test(`TH-CONTRAST ${theme} ${state} ${width}px text, controls, selected and keyboard focus`, async ({ page }) => {
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.clock.setFixedTime(new Date('2026-10-21T02:00:00Z'));
    await page.addInitScript(value => localStorage.setItem('unichope.theme',value),theme);
    await page.setViewportSize({ width, height: 1000 }); await fixture(page, state);
    const path = state === 'login' ? '/login' : state === 'tutor' || state === 'admin' ? '/' + state : state === 'notfound' ? '/unknown' : ['bookings','history','bookings-empty'].includes(state) ? '/student/bookings' : state === 'status' ? '/student/slots?week=2026-10-19&bad=1' : '/student/slots?week=2026-10-19';
    await page.goto(path); await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    if (['slots','dialog','success','status'].includes(state)) await expect(page.getByText(slot.tutorName)).toBeVisible();
    if (state === 'bookings') await expect(page.getByRole('button', { name: /^cancel(?: booking)?$/i })).toBeVisible();
    if (state === 'failure' || state === 'error' || state === 'status') await expect(page.getByRole('alert')).toBeVisible();
    if (state === 'dialog') { await page.getByRole('button', { name: /^book$/i }).click(); await expect(page.getByRole('dialog')).toBeVisible(); }
    if (state === 'history') { await page.getByRole('button',{name:/^history$/i}).click(); await expect(page.getByRole('article').getByText('Completed',{exact:true})).toBeVisible(); }
    if (state === 'bookings-empty') await expect(page.getByRole('button',{name:/^refresh$/i})).toBeVisible();
    if (state === 'success') { await page.getByRole('button',{name:/^book$/i}).click(); await page.getByRole('button',{name:/confirm/i}).click(); await expect(page.getByText(/booking confirmed/i)).toBeVisible(); }
    if (state === 'login-error') {
      // Start signed in, then make sign-out unconfirmed so Login contains both
      // the error notice and Retry sign out required by TH-CONTRAST.
      await page.unroute('**/api/**'); await fixture(page,'slots'); await page.goto('/student/slots?week=2026-10-19');
      await page.getByRole('button',{name:/^sign out$/i}).click(); await expect(page.getByRole('button',{name:/retry sign out/i})).toBeVisible();
    }
    await expect(page.locator('html')).toHaveAttribute('data-bs-theme',theme);
    expect(await page.locator('html').evaluate(el=>getComputedStyle(el).colorScheme)).toBe(theme);
    const text = await audit(page, 'text'); expect(text.checked).toBeGreaterThan(0); expect.soft(text.failures, 'all visible text').toEqual([]);
    const controls = await audit(page, 'controls'); expect.soft(controls.failures, 'control and selected boundaries').toEqual([]);
    // Visit the complete keyboard cycle, including the skip link; dialog cycling
    // is scoped by its focus trap. Audit text again in each focus-visible state.
    const count = await page.locator(state === 'dialog' ? '[role="dialog"] button' : 'a[href],button,input,select,textarea').count();
    let focused = 0;
    for (let index = 0; index < count + 1; index++) {
      await page.keyboard.press('Tab');
      const focus = await audit(page, 'focus'); focused += focus.checked;
      expect.soft(focus.failures, 'keyboard focus').toEqual([]);
      expect.soft((await audit(page, 'text')).failures, 'focused text').toEqual([]);
    }
    expect(focused).toBeGreaterThan(0);
  });
}

