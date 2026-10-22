import { createRequire } from 'node:module';
import { writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
const require = createRequire(new URL('../../../../../src/frontend/package.json', import.meta.url));
const { chromium } = require('@playwright/test');
const output = fileURLToPath(new URL('.', import.meta.url));
const browser = await chromium.launch({ channel: 'msedge', headless: true });
const observations = [];
const actor={id:'10000000-0000-4000-8000-000000000001',name:'Synthetic Student',role:'STUDENT'};
const moduleId='20000000-0000-4000-8000-000000000001';
const slot={id:'30000000-0000-4000-8000-000000000001',moduleId,moduleCode:'CS3227',moduleName:'Software Architecture',tutorName:'Synthetic Tutor',startTime:'2026-10-21T02:00:00Z',endTime:'2026-10-21T03:00:00Z'};
for (const theme of ['light','dark']) for (const width of [1440,320]) {
  const context=await browser.newContext({viewport:{width,height:1000}});
  const page=await context.newPage();
  await page.clock.setFixedTime(new Date('2026-10-21T02:00:00Z'));
  await page.addInitScript(theme=>localStorage.setItem('unichope.theme',theme),theme);
  await page.route('**/api/**', async route=>{
    const path=new URL(route.request().url()).pathname;
    const body=path==='/api/auth/me'?actor:path.endsWith('/modules')?[{id:moduleId,code:'CS3227',name:'Software Architecture'}]:path.endsWith('/slots')?[slot]:[];
    await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
  });
  await page.goto('http://127.0.0.1:4173/student/slots?week=2026-10-19');
  await page.getByText('Synthetic Tutor').waitFor();
  await page.screenshot({path:output+`implementer-${theme}-${width}.png`,fullPage:true});
  observations.push({kind:'screenshot',theme,width,overflow:await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth)});
  await context.close();
}
// Observe the production document with its application JS held back, then
// release it; repeat actual reloads and retain the root mutation timeline.
for (const path of ['/login','/student/slots','/admin']) {
 const context=await browser.newContext(); const page=await context.newPage();
 await page.addInitScript(()=>{
  localStorage.setItem('unichope.theme','dark');
  window.themeObservations=[];
  new MutationObserver(()=>{window.themeObservations.push({t:performance.now(),theme:document.documentElement?.getAttribute('data-bs-theme')})}).observe(document,{subtree:true,attributes:true,attributeFilter:['data-bs-theme']});
 });
 await page.route('**/api/**',route=>route.fulfill({status:401,contentType:'application/json',body:JSON.stringify({code:'UNAUTHENTICATED'})}));
 let release; const gate=new Promise(resolve=>release=resolve);
 await page.route('**/assets/*.js',async route=>{await gate;await route.continue()});
 await page.goto('http://127.0.0.1:4173'+path,{waitUntil:'commit'});
 await page.waitForFunction(()=>document.documentElement.getAttribute('data-bs-theme')==='dark');
 const held=await page.evaluate(()=>({theme:document.documentElement.getAttribute('data-bs-theme'),headings:document.querySelectorAll('h1').length,scheme:getComputedStyle(document.documentElement).colorScheme}));
 release(); await page.waitForLoadState('networkidle');
 const timeline=await page.evaluate(()=>window.themeObservations);
 await page.unroute('**/assets/*.js');
 await page.reload({waitUntil:'networkidle'});
 const reload=await page.evaluate(()=>({theme:document.documentElement.getAttribute('data-bs-theme'),scheme:getComputedStyle(document.documentElement).colorScheme,timeline:window.themeObservations}));
 observations.push({kind:'productionEarly',path,held,timeline,reload});
 await context.close();
}
await browser.close();
await writeFile(output+'implementer-production-observations.json',JSON.stringify(observations,null,2));
console.log(JSON.stringify(observations,null,2));
