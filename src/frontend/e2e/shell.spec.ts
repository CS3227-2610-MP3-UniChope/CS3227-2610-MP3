import { expect, test } from '@playwright/test';

test('FE0-QA: browser loads the UniChope shell without runtime errors', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await page.goto('/');
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  await expect(page.getByText('UniChope', { exact: true }).first()).toBeVisible();
  expect(errors).toEqual([]);
});
