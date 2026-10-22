import type { Page } from '@playwright/test';
const moduleId = '20000000-0000-4000-8000-000000000001';
const slot = { id: '30000000-0000-4000-8000-000000000001', moduleId, moduleCode: 'CS3227', moduleName: 'Software Architecture', tutorName: 'Contrast Synthetic Tutor', startTime: '2026-10-21T02:00:00Z', endTime: '2026-10-21T03:00:00Z' };
export async function fixture(page: Page, state: string) {
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname;
    let status = 200; let body: unknown;
    if (path === '/api/auth/me') {
      status = state === 'login' || state === 'login-error' ? 401 : state === 'failure' ? 503 : 200;
      body = status === 200 ? { id: '10000000-0000-4000-8000-000000000001', name: 'Contrast Synthetic Student', role: state === 'tutor' ? 'TUTOR' : state === 'admin' ? 'ADMIN' : 'STUDENT' } : { code: status === 401 ? 'UNAUTHENTICATED' : 'TEMPORARILY_UNAVAILABLE', requestId: 'contrast' };
    } else if (path === '/api/auth/logout') { status = 503; body = {code:'TEMPORARILY_UNAVAILABLE'}; } else if (path === '/api/auth/csrf') body = { headerName: 'X-TEST', token: 'test-only' };
    else if (path === '/api/student/modules') body = [{ id: moduleId, code: slot.moduleCode, name: slot.moduleName }];
    else if (path === '/api/student/slots') { if (state === 'loading') return; if (state === 'error') {status=503; body={code:'TEMPORARILY_UNAVAILABLE'};} else if (state === 'empty') body=[]; else body=[slot]; }
    else if (path === '/api/student/bookings') {
      if (route.request().method() === 'POST') { status=201; body={...slot,id:'40000000-0000-4000-8000-000000000001',slotId:slot.id,status:'ACTIVE',cancellable:true}; }
      else body = state === 'bookings-empty' ? [] : [{ ...slot, id: '40000000-0000-4000-8000-000000000001', slotId: slot.id, status: state === 'history' ? 'COMPLETED' : 'ACTIVE', cancellable: state !== 'history' }];
    }
    else { status = 404; body = { code: 'NOT_FOUND' }; }
    await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
  });
}
