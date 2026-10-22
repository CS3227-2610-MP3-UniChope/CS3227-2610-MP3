import { vi } from 'vitest';

export type Actor = { id: string; name: string; role: 'STUDENT' | 'TUTOR' | 'ADMIN' };
export const ids = {
  student: '10000000-0000-4000-8000-000000000001',
  module: '20000000-0000-4000-8000-000000000001',
  otherModule: '20000000-0000-4000-8000-000000000002',
  slot: '30000000-0000-4000-8000-000000000001',
  otherSlot: '30000000-0000-4000-8000-000000000002',
  booking: '40000000-0000-4000-8000-000000000001',
};
export const student: Actor = { id: ids.student, name: 'Synthetic Student', role: 'STUDENT' };
export const moduleOption = { id: ids.module, code: 'CS3227', name: 'Software Architecture' };
export const slot = {
  id: ids.slot, moduleId: ids.module, moduleCode: 'CS3227',
  moduleName: 'Software Architecture', tutorName: 'Synthetic Tutor',
  startTime: '2026-10-21T02:00:00Z', endTime: '2026-10-21T03:00:00Z',
};
export const booking = {
  ...slot, id: ids.booking, slotId: ids.slot, status: 'ACTIVE', cancellable: true,
};
export const json = (body: unknown, status = 200, headers: Record<string, string> = {}) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json', ...headers } });
export const failure = (status: number, code: string, headers: Record<string, string> = {}) =>
  json({ code, message: 'Safe synthetic failure', requestId: 'synthetic-request' }, status, headers);
export function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}
export type Call = { url: URL; method: string; init: RequestInit; body: unknown; headers: Headers };
type Handler = (call: Call) => Response | Promise<Response>;

// Installed only by tests. Runtime App receives no fixture or identity props.
export function apiFixture(initialActor: Actor | null = null) {
  let actor = initialActor;
  let csrfGeneration = 0;
  const calls: Call[] = [];
  const handlers = new Map<string, Handler>();
  let slots: unknown[] = [slot];
  let bookings: unknown[] = [booking];
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
    const request = input instanceof Request ? input : null;
    const url = new URL(request?.url ?? String(input), window.location.origin);
    const method = (init.method ?? request?.method ?? 'GET').toUpperCase();
    const rawBody = init.body ?? (request && method !== 'GET' ? await request.text() : null);
    const call: Call = {
      url, method, init, body: typeof rawBody === 'string' && rawBody ? JSON.parse(rawBody) : null,
      headers: new Headers(init.headers ?? request?.headers),
    };
    calls.push(call);
    const handler = handlers.get(`${method} ${url.pathname}`);
    if (handler) return handler(call);
    if (url.pathname === '/api/auth/me') return actor ? json(actor) : failure(401, 'UNAUTHENTICATED');
    if (url.pathname === '/api/auth/csrf') return json({ headerName: 'X-TEST-CSRF', token: `csrf-${++csrfGeneration}` });
    if (url.pathname === '/api/auth/login' && method === 'POST') { actor = student; return json(actor); }
    if (url.pathname === '/api/auth/logout' && method === 'POST') { actor = null; return new Response(null, { status: 204 }); }
    if (url.pathname === '/api/student/modules') return json([moduleOption, { id: ids.otherModule, code: 'CS9999', name: 'Other module' }]);
    if (url.pathname === '/api/student/slots') return json(slots);
    if (url.pathname === '/api/student/bookings' && method === 'GET') return json(bookings);
    if (url.pathname === '/api/student/bookings' && method === 'POST') return json(booking, 201);
    if (url.pathname === `/api/student/bookings/${ids.booking}/cancel` && method === 'POST') return json({ ...booking, status: 'CANCELLED', cancellable: false });
    throw new Error(`Unconfigured test-only API: ${method} ${url.pathname}`);
  });
  vi.stubGlobal('fetch', fetchMock);
  return {
    calls, fetchMock,
    givenActor(value: Actor | null) { actor = value; },
    givenSlots(value: unknown[]) { slots = value; },
    givenBookings(value: unknown[]) { bookings = value; },
    on(method: string, path: string, handler: Handler) { handlers.set(`${method} ${path}`, handler); },
    requests(path: string, method?: string) { return calls.filter(call => call.url.pathname === path && (!method || call.method === method)); },
  };
}
