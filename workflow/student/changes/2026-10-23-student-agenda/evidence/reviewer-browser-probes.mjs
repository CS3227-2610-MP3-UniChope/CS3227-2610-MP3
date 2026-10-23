import { chromium } from '../../../../../src/frontend/node_modules/@playwright/test/index.mjs';
import { writeFile } from 'node:fs/promises';
const browser=await chromium.launch({channel:'msedge',headless:true});
const id='40000000-0000-4000-8000-000000000001';
const actor={id:'10000000-0000-4000-8000-000000000001',name:'Synthetic Student',role:'STUDENT'};
const booking={id,slotId:'30000000-0000-4000-8000-000000000001',moduleId:'20000000-0000-4000-8000-000000000001',moduleCode:'CS3227',moduleName:'Architecture',tutorName:'Synthetic Tutor',startTime:'2099-10-21T02:00:00Z',endTime:'2099-10-21T03:00:00Z',status:'ACTIVE',cancellable:true};
const observations=[];
try {
 const page=await browser.newPage(); let calls=0,release;
 await page.route('**/api/**',async route=>{
  const path=new URL(route.request().url()).pathname; let body;
  if(path==='/api/auth/me')body=actor;
  else if(path==='/api/auth/csrf')body={headerName:'X-TEST-CSRF',token:'synthetic'};
  else if(path.endsWith('/agenda')){ calls++; if(calls===2) await new Promise(resolve=>release=resolve); body={bookingId:id,agenda:calls===1?'First generated agenda':'Second generated agenda',requiresReview:true,providerMode:'fake'}; }
  else if(path==='/api/student/bookings')body=[booking]; else if(path.endsWith('/modules')||path.endsWith('/slots'))body=[]; else body=booking;
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
 });
 await page.goto('http://127.0.0.1:5173/student/bookings');
 await page.getByRole('link',{name:'Prepare agenda',exact:true}).click();
 const q=page.getByRole('textbox',{name:'Your topics and questions'});await q.fill('Synthetic topic');
 await page.getByRole('button',{name:'Generate agenda',exact:true}).click();
 const editor=page.getByRole('textbox',{name:'Agenda draft'});await editor.waitFor();
 await page.getByRole('button',{name:'Regenerate agenda',exact:true}).click();
 await page.waitForFunction(()=>document.querySelector('[role=status]')?.textContent?.includes('Generating'));
 await editor.fill('My important edit while request is pending'); release();
 await page.waitForFunction(()=>document.querySelector('#agenda-draft')?.value==='Second generated agenda');
 observations.push({probe:'pending-regeneration-overwrites-new-edit',actual:await editor.inputValue(),confirmationDialogs:await page.getByRole('dialog').count(),expected:'preserve edit or require confirmation'});
 await page.goBack(); await page.waitForURL('**/student/bookings');
 observations.push({probe:'browser-back-discards-dirty-preparation',actualPath:new URL(page.url()).pathname,confirmationDialogs:await page.getByRole('dialog').count(),expected:'warn before leaving dirty preparation'});
 await page.close();
} finally { await browser.close(); }
await writeFile(new URL('reviewer-browser-probes.json',import.meta.url),JSON.stringify(observations,null,2));
console.log(JSON.stringify(observations));
