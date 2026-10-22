// Reviewer probe: imports the actual src/frontend/src/api.ts; synthetic fetch only.
const mod = await import(process.argv[2]);
const { ApiClient, ApiError, StaleRequest } = mod;
type R = { status: number; code?: string };
function respond(r: R) {
  if (r.status === 204) return new Response(null, { status: 204 });
  const body = r.status < 400 ? { headerName: 'X-P', token: 't1' } : { code: r.code, message: 'x', requestId: 'p' };
  return new Response(JSON.stringify(body), { status: r.status, headers: { 'Content-Type': 'application/json' } });
}
async function run(label: string, csrf: R, path: string, protectedRequest: boolean, post: R = { status: 201 }) {
  const calls: string[] = []; let sessionErrors = 0;
  (globalThis as any).fetch = async (p: string, init: RequestInit) => { calls.push(`${init.method} ${p}`); return respond(p === '/api/auth/csrf' ? csrf : post); };
  const api = new ApiClient(); api.onSessionError = () => { sessionErrors++; api.reset(); };
  let outcome = 'resolved';
  try { await api.request(path, path.endsWith('/cancel') ? {} : path === '/api/auth/logout' ? {} : { slotId: 's' }, protectedRequest); }
  catch (e) { outcome = e instanceof StaleRequest ? 'StaleRequest' : e instanceof ApiError ? `ApiError(${e.status},${e.code})` : String(e); }
  const posts = calls.filter(c => c.startsWith('POST')).length;
  console.log(JSON.stringify({ label, sessionErrors, posts, outcome, calls }));
}
await run('protected book, csrf 401', { status: 401, code: 'UNAUTHENTICATED' }, '/api/student/bookings', true);
await run('protected book, csrf 403 inactive', { status: 403, code: 'ACCOUNT_INACTIVE' }, '/api/student/bookings', true);
await run('protected cancel, csrf 401', { status: 401, code: 'UNAUTHENTICATED' }, '/api/student/bookings/b/cancel', true);
await run('protected cancel, csrf 403 inactive', { status: 403, code: 'ACCOUNT_INACTIVE' }, '/api/student/bookings/b/cancel', true);
await run('protected book, csrf 503', { status: 503, code: 'TEMPORARILY_UNAVAILABLE' }, '/api/student/bookings', true);
await run('protected book, csrf 403 FORBIDDEN', { status: 403, code: 'FORBIDDEN' }, '/api/student/bookings', true);
await run('anonymous login, csrf 401', { status: 401, code: 'UNAUTHENTICATED' }, '/api/auth/login', false);
await run('anonymous login, csrf 403 inactive', { status: 403, code: 'ACCOUNT_INACTIVE' }, '/api/auth/login', false);
await run('anonymous logout, csrf 403 inactive', { status: 403, code: 'ACCOUNT_INACTIVE' }, '/api/auth/logout', false);
await run('protected book, POST 401 (token ok)', { status: 200 }, '/api/student/bookings', true, { status: 401, code: 'UNAUTHENTICATED' });
await run('protected book, POST 403 CSRF_INVALID', { status: 200 }, '/api/student/bookings', true, { status: 403, code: 'CSRF_INVALID' });
