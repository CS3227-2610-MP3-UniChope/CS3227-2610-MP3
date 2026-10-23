import { chromium, expect } from '../../../../../src/frontend/node_modules/@playwright/test/index.mjs';
import { writeFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
const browser=await chromium.launch({channel:'msedge',headless:true});
const id='40000000-0000-4000-8000-000000000001';
const booking={id,slotId:'30000000-0000-4000-8000-000000000001',moduleId:'20000000-0000-4000-8000-000000000001',moduleCode:'CS3227',moduleName:'Architecture',tutorName:'Synthetic Tutor',startTime:'2099-10-21T02:00:00Z',endTime:'2099-10-21T03:00:00Z',status:'ACTIVE',cancellable:true};
const results=[];
try {
 const page=await browser.newPage(); let calls=0,release;
 await page.route('**/api/**',async route=>{
  const path=new URL(route.request().url()).pathname; let body;
  if(path==='/api/auth/me')body={id:'10000000-0000-4000-8000-000000000001',name:'Synthetic Student',role:'STUDENT'};
  else if(path==='/api/auth/csrf')body={headerName:'X-TEST-CSRF',token:'synthetic'};
  else if(path.endsWith('/agenda')){ calls++; if(calls===2) await new Promise(resolve=>release=resolve); body={bookingId:id,agenda:calls===1?'First generated agenda':'Second generated agenda',requiresReview:true,providerMode:'fake'}; }
  else if(path==='/api/student/bookings')body=[booking]; else if(path.endsWith('/modules')||path.endsWith('/slots'))body=[]; else body=booking;
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
 });
 const agenda=`/student/bookings/${id}/agenda`;
 await page.goto('http://127.0.0.1:5173/student/bookings');
 await page.getByRole('link',{name:'Prepare agenda',exact:true}).click();
 const q=page.getByRole('textbox',{name:'Your topics and questions'});await q.fill('Synthetic topic');
 await page.getByRole('button',{name:'Generate agenda',exact:true}).click();
 const editor=page.getByRole('textbox',{name:'Agenda draft'});await editor.waitFor();
 await page.getByRole('button',{name:'Regenerate agenda',exact:true}).click();
 await expect(editor).toHaveAttribute('readonly','');
 await editor.focus(); await page.keyboard.type('Attempted edit');
 assert.equal(await editor.inputValue(),'First generated agenda');release();
 await expect(editor).toHaveValue('Second generated agenda');
 await expect(editor).not.toHaveAttribute('readonly','');
 results.push({finding:'AG-R1',pendingDraftReadOnly:true,acceptedEditsLost:false,requests:calls});
 const dialog=page.getByRole('dialog');
 await page.evaluate(()=>history.back());await dialog.waitFor();await page.keyboard.press('Escape');
 await expect(page).toHaveURL(new RegExp(agenda+'$'));await expect(q).toHaveValue('Synthetic topic');
 await page.evaluate(()=>history.back());await dialog.waitFor();await dialog.getByRole('button',{name:'Leave page'}).click();
 await expect(page).toHaveURL(/\/student\/bookings$/);
 await page.evaluate(()=>history.forward());await q.waitFor();await expect(q).toHaveValue('');
 await page.getByRole('link',{name:'Find a consultation',exact:true}).click();await expect(page).toHaveURL(/\/student\/slots$/);
 await page.evaluate(()=>history.back());await q.waitFor();await q.fill('Unsaved forward questions');
 await page.evaluate(()=>history.forward());await dialog.waitFor();await page.keyboard.press('Escape');
 await expect(page).toHaveURL(new RegExp(agenda+'$'));await expect(q).toHaveValue('Unsaved forward questions');
 await page.evaluate(()=>history.forward());await dialog.waitFor();await dialog.getByRole('button',{name:'Leave page'}).click();
 await expect(page).toHaveURL(/\/student\/slots$/);
 results.push({finding:'AG-R2',backDeclinePreserves:true,backConfirmLeaves:true,forwardDeclinePreserves:true,forwardConfirmLeaves:true});
 await page.close();
} finally {await browser.close();}
await writeFile(new URL('browser-corrections.json',import.meta.url),JSON.stringify(results,null,2));
console.log(JSON.stringify(results));
