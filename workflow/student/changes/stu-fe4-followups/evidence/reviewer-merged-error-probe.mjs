import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';
const require = createRequire(new URL('../../../../../src/frontend/package.json', import.meta.url));
const { chromium } = require('@playwright/test');
const { createServer } = await import(pathToFileURL(require.resolve('vite')).href);
const server = await createServer({ root: new URL('../../../../../src/frontend/', import.meta.url).pathname.replace(/^\/(\w:)/, '$1'), server: { host: '127.0.0.1', port: 5174, strictPort: true } });
await server.listen();
const browser = await chromium.launch({ channel: 'msedge' });
const results = [];
try {
  for (const scenario of ['modulesForbiddenSlotsSuccess', 'modulesForbiddenSlotsUnavailable', 'modulesForbiddenSlotsMalformed']) {
    const page = await browser.newPage();
    await page.route('**/api/**', async route => {
      const path = new URL(route.request().url()).pathname;
      let status = 200, body = [];
      if (path === '/api/auth/me') body = { id: 'synthetic-review-student', name: 'Synthetic Review Student', role: 'STUDENT' };
      else if (path === '/api/student/modules') { status = 403; body = { code: 'FORBIDDEN' }; }
      else if (path === '/api/student/slots' && scenario.endsWith('Unavailable')) { status = 503; body = { code: 'TEMPORARILY_UNAVAILABLE' }; }
      else if (path === '/api/student/slots' && scenario.endsWith('Malformed')) body = { unexpected: true };
      await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
    });
    await page.goto('http://127.0.0.1:5174/student/slots?week=2026-10-19');
    await page.getByRole('alert').waitFor();
    // Both reads must finish before collecting the final merged state.
    await page.waitForFunction(() => !document.body.textContent.includes('Loading modules') && !document.body.textContent.includes('Loading consultations'));
    results.push({ scenario, alert: await page.getByRole('alert').innerText(), homeLink: await page.getByRole('link', { name: 'Back to home', exact: true }).count(), retry: await page.getByRole('button', { name: 'Retry', exact: true }).count(), moduleFilter: await page.getByLabel('Module', { exact: true }).count() });
    await page.close();
  }
  console.log(JSON.stringify(results, null, 2));
} finally { await browser.close(); await server.close(); }
