import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';
const tools = createRequire(path.join(process.env.QA_TOOLS || path.dirname(new URL(import.meta.url).pathname), 'package.json'));
const { chromium, expect } = tools('@playwright/test');
const axeModule = tools('@axe-core/playwright');
const AxeBuilder = axeModule.default || axeModule;
const password = process.env.QA_PASSWORD;
if (!password) throw new Error('QA_PASSWORD must identify the disposable synthetic fixture.');
const output = process.env.QA_OUTPUT;
fs.mkdirSync(output, { recursive: true });
const result = { environment: 'isolated disposable PostgreSQL and actual backend, synthetic fixtures only', steps: [], accessibility: [], mutations: [], errors: [], overflow: [] };
const browser = await chromium.launch({ channel: 'msedge', headless: true });
const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, timezoneId: 'America/Los_Angeles' });
const page = await context.newPage();
page.on('pageerror', e => result.errors.push(e.message));
page.on('response', async response => {
 const request = response.request(); const pathname = new URL(request.url()).pathname;
 if(request.method()==='POST' && pathname.startsWith('/api/student/')) {
  result.mutations.push({ operation: pathname.endsWith('/cancel') ? 'cancel' : 'book', status: response.status(), bodyKeys: Object.keys(request.postDataJSON() || {}), csrfPresent: !!request.headers()['x-csrf-token'] });
 }
});
const mark = name => { result.steps.push(name); console.log('PASS: '+name); };
async function snapshot(name) { await page.screenshot({ path: path.join(output, name+'.png'), fullPage: true }); }
async function accessibility(name) {
 const scan = await new AxeBuilder({ page }).withTags(['wcag2a','wcag2aa','wcag21a','wcag21aa']).analyze();
 result.accessibility.push({ name, violations: scan.violations.map(v=>({ id:v.id, impact:v.impact, description:v.description, targets:v.nodes.map(n=>n.target) })) });
}
async function signIn(email) {
 await page.getByLabel(/^Email$/i).fill(email);
 await page.getByLabel(/^Password$/i).fill(password);
 await page.getByRole('button', {name:/^Sign in$/i}).click();
 await expect(page.getByRole('button',{name:/Sign out/i})).toBeVisible();
}
try {
 await page.goto('http://127.0.0.1:5175/login');
 await expect(page.getByRole('button',{name:/^Sign in$/i})).toBeVisible();
 await snapshot('live-login-desktop'); await accessibility('login'); mark('Actual login page rendered');
 await signIn('student-qa@unichope.example');
 await expect(page).toHaveURL(/\/student\/slots/);
 await expect(page.getByRole('button',{name:/^Book$/i}).first()).toBeVisible();
 await snapshot('live-timetable-desktop'); await accessibility('student-timetable'); mark('Real Student session and eligible slots loaded through Vite proxy');
 for (const width of [320,768,1024,1440]) {
  await page.setViewportSize({width,height:1000});
  await page.waitForTimeout(120);
  const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>window.innerWidth+1);
  result.overflow.push({width,overflow}); expect(overflow).toBe(false);
  if(width===320){await snapshot('live-timetable-mobile'); await accessibility('student-mobile');}
 }
 await page.setViewportSize({width:1440,height:1000});
 const book = page.getByRole('button',{name:/^Book$/i}).first();
 await book.click();
 await expect(page.getByRole('dialog')).toBeVisible();
 await snapshot('live-booking-confirmation'); await accessibility('booking-confirmation');
 await page.getByRole('button',{name:/^Confirm booking$/i}).click();
 await expect(page.getByText(/Booking confirmed\./)).toBeVisible(); mark('Real booking committed after explicit confirmation');
 await page.getByRole('link',{name:/^My bookings$/i}).click();
 await expect(page.getByRole('button',{name:/^Cancel booking$/i}).first()).toBeVisible();
 await snapshot('live-my-bookings'); await accessibility('my-bookings');
 await page.getByRole('button',{name:/^Cancel booking$/i}).first().click();
 await expect(page.getByRole('dialog')).toBeVisible();
 await page.getByRole('button',{name:/^Confirm cancellation$/i}).click();
 await expect(page.getByText(/Booking cancelled\./)).toBeVisible(); mark('Real cancellation committed');
 await page.getByRole('button',{name:/^History$/i}).click();
 await expect(page.getByRole('region', { name: 'Your bookings' }).getByText(/^Cancelled$/).first()).toBeVisible(); await snapshot('live-history'); mark('Cancelled booking retained in History');
 await page.getByRole('button',{name:/Sign out/i}).click();
 await expect(page.getByRole('button',{name:/^Sign in$/i})).toBeVisible(); mark('Real Student sign-out cleared workspace');
 for(const [role,email] of [['Tutor','tutor-one-qa@unichope.example'],['Admin','admin-qa@unichope.example']]) {
  await signIn(email); await expect(page.getByRole('heading',{level:1,name:new RegExp(role,'i')})).toBeVisible();
  await expect(page.getByText(/workspace is not ready yet/i)).toBeVisible();
  await accessibility(role.toLowerCase()+'-placeholder'); await snapshot('live-'+role.toLowerCase()+'-placeholder');
  mark('Actual '+role+' login routed to honest placeholder');
  await page.getByRole('button',{name:/Sign out/i}).click(); await expect(page.getByRole('button',{name:/^Sign in$/i})).toBeVisible();
 }
 const stored = await page.evaluate(()=>({local:localStorage.length,session:sessionStorage.length}));
 expect(stored).toEqual({local:0,session:0}); result.storage=stored; mark('No private browser storage persisted');
 expect(result.mutations.filter(m=>m.operation==='book')).toHaveLength(1);
 expect(result.mutations.filter(m=>m.operation==='cancel')).toHaveLength(1);
 expect(result.mutations.every(m=>m.csrfPresent && (m.operation==='book'?m.status===201:m.status===200))).toBe(true);
 expect(result.errors).toHaveLength(0);
 result.passed = true;
} catch(error) {
 result.passed=false; result.failure=error.message; console.error(error.message); await snapshot('live-failure'); process.exitCode=1;
} finally {
 fs.writeFileSync(path.join(output,'live-browser-smoke.json'), JSON.stringify(result,null,2)); await browser.close();
 console.log('Accessibility violations: '+result.accessibility.reduce((n,r)=>n+r.violations.length,0));
}
