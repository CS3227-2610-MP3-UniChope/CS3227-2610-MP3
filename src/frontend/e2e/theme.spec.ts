import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';
const authoredIndex = readFileSync(new URL('../index.html', import.meta.url), 'utf8');
import { fixture } from './theme-fixture';
for (const path of ['/login','/student/slots','/admin']) for (const value of ['dark','light','absent','Dark','blue','','{"theme":"dark"}','read-error','access-error']) {
 test(`TH-NOFLASH ${path} ${JSON.stringify(value)} before bundle`,async({page})=>{
  await page.addInitScript(value=>{
   localStorage.removeItem('unichope.theme');
   if(value==='read-error') Storage.prototype.getItem=()=>{throw new Error('blocked')};
   else if(value==='access-error') Object.defineProperty(window,'localStorage',{get(){throw new Error('blocked')}});
   else if(value!=='absent') localStorage.setItem('unichope.theme',value);
  },value);
  // Hold every module (Vite client included), but allow synchronous head scripts.
  await page.route('**/*',async route=>{
   if(route.request().resourceType()==='script' && /(?:\.tsx?(?:\?|$)|@vite\/client)/.test(new URL(route.request().url()).pathname)) await route.abort();
   else await route.continue();
  });
  await page.goto(path,{waitUntil:'domcontentloaded'});
  await expect(page.locator('html')).toHaveAttribute('data-bs-theme',value==='dark'?'dark':'light');
  const scripts=await page.locator('head script').evaluateAll(nodes=>nodes.map(node=>({src:(node as HTMLScriptElement).src,type:(node as HTMLScriptElement).type,async:(node as HTMLScriptElement).async,defer:(node as HTMLScriptElement).defer,text:node.textContent})));
  const moduleIndex=scripts.findIndex(s=>s.type==='module' && s.src.includes('/src/main'));
  expect(moduleIndex).toBeGreaterThan(0);
  const early=scripts.slice(0,moduleIndex).filter(s=>s.src && s.type!=='module');
  expect(early.length).toBeGreaterThan(0);
  expect(early.some(s=>new URL(s.src).origin===new URL(page.url()).origin&&!s.async&&!s.defer)).toBe(true);
  // S3 concerns authored index.html; Vite injects an inline React Refresh module in development.
  // Inspect the source without exemptions, while the served-DOM checks above verify execution order.
  const authoredInline=await page.evaluate(source=>Array.from(new DOMParser().parseFromString(source,'text/html').querySelectorAll('script')).filter(s=>!s.hasAttribute('src')&&s.textContent?.trim()).map(s=>s.textContent),authoredIndex);
  expect(authoredInline).toEqual([]);
  await expect(page.getByRole('heading')).toHaveCount(0);
 });
}
for(const scheme of ['dark','light'] as const) for(const state of ['login','slots','admin']) test(`TH-DEFAULT ${state} ignores ${scheme} device and changes`,async({page})=>{
 await page.emulateMedia({colorScheme:scheme});await fixture(page,state);await page.goto(state==='login'?'/login':state==='admin'?'/admin':'/student/slots?week=2026-10-19');
 await expect(page.getByRole('heading',{level:1})).toBeVisible();await expect(page.locator('html')).toHaveAttribute('data-bs-theme','light');
 await page.emulateMedia({colorScheme:scheme==='dark'?'light':'dark'});await expect(page.locator('html')).toHaveAttribute('data-bs-theme','light');
 await expect(page.getByRole('button',{name:'Dark theme',exact:true})).toHaveAttribute('aria-pressed','false');
});
for(const width of [320,768,1024,1440]) test(`TH-A11Y ${width}px keyboard and dialog`,async({page})=>{
 await page.setViewportSize({width,height:1000});await fixture(page,'slots');await page.goto('/student/slots?week=2026-10-19');
 const button=page.getByRole('button',{name:'Dark theme',exact:true});await expect(button).toBeVisible();
 const count=await page.locator('a[href],button,input,select').count();let reached=false;
 for(let i=0;i<count+1;i++){await page.keyboard.press('Tab');if(await button.evaluate(el=>el===document.activeElement)){reached=true;break;}}
 expect(reached).toBe(true);await page.keyboard.press('Enter');await expect(button).toBeFocused();await expect(button).toHaveAttribute('aria-pressed','true');
 await page.keyboard.press('Space');await expect(button).toBeFocused();await expect(button).toHaveAttribute('aria-pressed','false');
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 expect(await page.getByRole('banner').evaluate(el=>el.scrollWidth<=el.clientWidth)).toBe(true);
 await page.getByRole('button',{name:/^book$/i}).click();const dialog=page.getByRole('dialog');await expect(dialog).toBeVisible();
 for(let i=0;i<8;i++){await page.keyboard.press('Tab');expect(await dialog.evaluate(el=>el.contains(document.activeElement))).toBe(true);}
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.keyboard.press('Escape');await expect(dialog).toHaveCount(0);await expect(page.getByRole('button',{name:/^book$/i})).toBeFocused();
});
test('TH-PERSIST real reload and app URL navigation remember both choices',async({page})=>{
 await fixture(page,'login');await page.goto('/login');const button=page.getByRole('button',{name:'Dark theme',exact:true});await button.click();
 await page.reload();await expect(button).toHaveAttribute('aria-pressed','true');await page.goto('/unknown');await expect(button).toHaveAttribute('aria-pressed','true');
 await button.click();await page.reload();await expect(button).toHaveAttribute('aria-pressed','false');
});
