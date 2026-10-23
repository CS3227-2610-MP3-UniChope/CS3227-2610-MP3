import { chromium } from '../../../../../src/frontend/node_modules/@playwright/test/index.mjs';
import { writeFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
const browser = await chromium.launch({ channel: 'msedge', headless: true });
const id = '40000000-0000-4000-8000-000000000001';
const booking = { id, slotId: '30000000-0000-4000-8000-000000000001', moduleId: '20000000-0000-4000-8000-000000000001', moduleCode: 'CS3227', moduleName: 'Software Architecture', tutorName: 'Synthetic Tutor', startTime: '2099-10-21T02:00:00Z', endTime: '2099-10-21T03:00:00Z', status: 'ACTIVE', cancellable: true };
const results = [];
try {
  for (const theme of ['light', 'dark']) for (const width of [320,768,1024,1440]) {
    const page = await browser.newPage({ viewport: { width, height: 1000 } });
    const errors = []; let posts=0;
    page.on('pageerror', error => errors.push(error.message));
    await page.addInitScript(theme => localStorage.setItem('unichope.theme', theme), theme);
    await page.route('**/api/**', async route => {
      const path = new URL(route.request().url()).pathname;
      let body;
      if (path === '/api/auth/me') body={id:'10000000-0000-4000-8000-000000000001',name:'Synthetic Student',role:'STUDENT'};
      else if (path === '/api/auth/csrf') body={headerName:'X-TEST-CSRF',token:'synthetic'};
      else if (path.endsWith('/agenda')) { posts++; body={ bookingId:id,agenda:'Goal\nClarify recursion\n\nAgenda (60 min)\n1. Base cases (10 min)\n   How do base cases work?',requiresReview:true,providerMode:'fake' }; }
      else body=booking;
      await route.fulfill({ status:200, contentType:'application/json', body:JSON.stringify(body) });
    });
    await page.goto(`http://127.0.0.1:5173/student/bookings/${id}/agenda`);
    const questions=page.getByRole('textbox',{name:/topics and questions/i});
    await questions.waitFor(); await questions.fill('How do base cases work?');
    const generate=page.getByRole('button',{name:'Generate agenda'});
    await generate.focus(); await page.keyboard.press('Enter');
    const draft=page.getByRole('textbox',{name:'Agenda draft'}); await draft.waitFor();
    await draft.fill('My reviewed synthetic draft');
    await page.getByRole('button',{name:'Regenerate agenda'}).click();
    const dialog=page.getByRole('dialog'); await dialog.waitFor();
    for (const key of ['Tab','Tab','Shift+Tab','Tab']) { await page.keyboard.press(key); assert(await dialog.evaluate(el=>el.contains(document.activeElement))); }
    await page.keyboard.press('Escape'); assert.equal(await dialog.count(),0);
    assert.equal(await draft.inputValue(),'My reviewed synthetic draft'); assert.equal(posts,1);
    await page.getByRole('link',{name:'My bookings',exact:true}).click(); await dialog.waitFor();
    await page.keyboard.press('Escape'); assert.equal(await questions.inputValue(),'How do base cases work?');
    assert(await page.evaluate(()=>document.documentElement.scrollWidth <= innerWidth+1));
    assert.deepEqual(errors,[]);
    assert(await page.evaluate(()=>!JSON.stringify(localStorage).includes('synthetic')&&!JSON.stringify(sessionStorage).includes('synthetic')));
    if(width===1440) await page.screenshot({path:new URL(`agenda-${theme}.png`,import.meta.url).pathname.slice(1), fullPage:true});
    results.push({theme,width,keyboardFocusTrap:true,escapedWithoutLoss:true,noOverflow:true,noPageErrors:true,posts});
    await page.close();
  }
  await writeFile(new URL('browser-agenda.json',import.meta.url), JSON.stringify(results,null,2));
  console.log(JSON.stringify({passed:results.length, widths:[320,768,1024,1440],themes:['light','dark']}));
} finally { await browser.close(); }
