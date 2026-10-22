export type Actor = { id: string; name: string; role: 'STUDENT' | 'TUTOR' | 'ADMIN' };
export type Module = { id: string; code: string; name: string };
export type Slot = { id: string; moduleId: string; moduleCode: string; moduleName: string; tutorName: string; startTime: string; endTime: string };
export type Booking = Slot & { slotId: string; status: 'ACTIVE' | 'CANCELLED' | 'COMPLETED'; cancellable: boolean };
export class ApiError extends Error {
  constructor(public status = 0, public code = 'UNAVAILABLE', public retryAfter = '') { super(code); }
}
export class StaleRequest extends Error {}
export class PreflightError extends ApiError {
  constructor() { super(0, 'NOTHING_SENT'); }
}
export const isActor = (value: unknown): value is Actor => {
  const a = value as Actor | null;
  return !!a && typeof a.id === 'string' && typeof a.name === 'string' && ['STUDENT', 'TUTOR', 'ADMIN'].includes(a.role);
};
export class ApiClient {
  private generation = 0;
  private controllers = new Set<AbortController>();
  private token: { headerName: string; token: string } | null = null;
  private tokenPromise: Promise<void> | null = null;
  onSessionError: (error: ApiError) => void = () => {};
  reset() {
    this.generation++;
    this.controllers.forEach(controller => controller.abort());
    this.controllers.clear(); this.token = null; this.tokenPromise = null;
  }
  clearToken() { this.token = null; this.tokenPromise = null; }
  async csrf() {
    if (this.token) return;
    if (!this.tokenPromise) {
      const generation = this.generation;
      this.tokenPromise = this.request<unknown>('/api/auth/csrf', undefined, false).then(value => {
        const t = value as { headerName?: unknown; token?: unknown };
        if (!t || typeof t.headerName !== 'string' || !/^[!#$%&'*+.^_`|~0-9A-Za-z-]+$/.test(t.headerName) || typeof t.token !== 'string' || !t.token) throw new ApiError();
        if (generation !== this.generation) throw new StaleRequest();
        this.token = { headerName: t.headerName, token: t.token };
      }).finally(() => { if (generation === this.generation) this.tokenPromise = null; });
    }
    return this.tokenPromise;
  }
  async request<T>(path: string, body?: object, protectedRequest = true, method = body === undefined ? 'GET' : 'POST'): Promise<T> {
    const generation = this.generation;
    const unsafe = !['GET', 'HEAD'].includes(method);
    if (unsafe) {
      try { await this.csrf(); }
      catch (error) {
        if (generation !== this.generation) throw new StaleRequest();
        // Token acquisition is shared with anonymous login/logout. A protected
        // caller must still honor session revocation before its POST is sent.
        if (protectedRequest && error instanceof ApiError && (error.status === 401 || error.code === 'ACCOUNT_INACTIVE')) {
          this.onSessionError(error);
          if (generation !== this.generation) throw new StaleRequest();
        }
        if (!protectedRequest && error instanceof ApiError && (error.status === 401 || (error.status === 403 && error.code === 'ACCOUNT_INACTIVE'))) {
          this.clearToken();
          throw error;
        }
        throw new PreflightError();
      }
    }
    if (generation !== this.generation) throw new StaleRequest();
    const controller = new AbortController(); this.controllers.add(controller);
    try {
      const headers: Record<string, string> = { Accept: 'application/json' };
      if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
      }
      if (unsafe) headers[this.token!.headerName] = this.token!.token;
      const response = await fetch(path, { method, headers,
        ...(body === undefined ? {} : { body: JSON.stringify(body) }), credentials: 'same-origin', cache: 'no-store', signal: controller.signal });
      if (generation !== this.generation) throw new StaleRequest();
      const assignmentWrite = path.startsWith('/api/admin/assignments/') && ['PUT', 'DELETE'].includes(method);
      const created = method === 'POST' && ['/api/student/bookings', '/api/auth/register', '/api/admin/users', '/api/admin/modules'].includes(path);
      const expectedStatus = assignmentWrite || (method === 'POST' && path === '/api/auth/logout') ? 204 : created ? 201 : 200;
      if (response.ok && response.status !== expectedStatus) throw new ApiError();
      if (response.status === 204) return undefined as T;
      if (!response.headers.get('Content-Type')?.includes('application/json')) throw new ApiError();
      let value: unknown;
      try { value = await response.json(); } catch { throw new ApiError(); }
      if (generation !== this.generation) throw new StaleRequest();
      if (!response.ok) {
        const code = value && typeof value === 'object' && 'code' in value && typeof value.code === 'string' ? value.code : 'UNAVAILABLE';
        const error = new ApiError(response.status, code, response.headers.get('Retry-After') || '');
        if (code === 'CSRF_INVALID') this.clearToken();
        if (protectedRequest && (response.status === 401 || code === 'ACCOUNT_INACTIVE')) this.onSessionError(error);
        throw error;
      }
      return value as T;
    } catch (error) {
      if (generation !== this.generation || controller.signal.aborted) throw new StaleRequest();
      throw error instanceof ApiError || error instanceof StaleRequest ? error : new ApiError();
    } finally { this.controllers.delete(controller); }
  }
}
export function safeError(error: unknown, mutation = false) {
  if (error instanceof PreflightError) return 'Nothing was changed. Please try again.';
  const e = error instanceof ApiError ? error : new ApiError();
  if (e.code === 'CSRF_INVALID') return 'Your security token expired. Retry with a fresh token to continue.';
  if (e.code === 'FORBIDDEN') return 'Access denied. You do not have permission to view this resource.';
  if (e.code === 'SLOT_UNAVAILABLE') return 'This slot is no longer available. The list has been refreshed; choose another consultation.';
  if (e.code === 'BOOKING_OVERLAP') return 'This consultation overlaps another active booking. Choose a different time.';
  if (e.code === 'NOT_FOUND' || e.code === 'INVALID_STATE') return 'This booking is no longer available for cancellation. The list has been refreshed.';
  if (mutation) return 'We could not confirm the outcome. The change may have completed. Refresh and check My bookings before trying again.';
  return 'The service is temporarily unavailable. Please try again.';
}
