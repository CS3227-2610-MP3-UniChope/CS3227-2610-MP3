import { afterEach, expect, test, vi } from 'vitest';
import { ApiClient, ApiError, PreflightError } from '../api';
import { failure, json } from './api-fixture';
afterEach(() => vi.unstubAllGlobals());

test.each(['PUT', 'DELETE'])('Admin %s acquires CSRF even with no body and accepts assignment 204', async method => {
  const fetcher = vi.fn(async (path: string, init?: RequestInit) => path === '/api/auth/csrf' ? json({ headerName: 'X-CSRF-TOKEN', token: 'safe-token' }) : new Response(null, { status: 204 }));
  vi.stubGlobal('fetch', fetcher);
  await new ApiClient().request('/api/admin/assignments/tutor/module', method === 'PUT' ? {} : undefined, true, method);
  const init = fetcher.mock.calls[1][1]!;
  expect(init.method).toBe(method);
  expect(new Headers(init.headers).get('X-CSRF-TOKEN')).toBe('safe-token');
  expect(init.body).toBe(method === 'PUT' ? '{}' : undefined);
});

test.each(['/api/admin/users', '/api/admin/modules', '/api/auth/register'])('Create %s requires 201', async path => {
  vi.stubGlobal('fetch', vi.fn(async (url: string) => url === '/api/auth/csrf' ? json({ headerName: 'X-CSRF-TOKEN', token: 'safe-token' }) : json({ message: 'Created' }, 201)));
  await expect(new ApiClient().request(path, {}, false)).resolves.toEqual({ message: 'Created' });
});

test('Failed CSRF preflight never sends an assignment deletion', async () => {
  const fetcher = vi.fn(async () => failure(503, 'TEMPORARILY_UNAVAILABLE'));
  vi.stubGlobal('fetch', fetcher);
  await expect(new ApiClient().request('/api/admin/assignments/tutor/module', undefined, true, 'DELETE')).rejects.toBeInstanceOf(PreflightError);
  expect(fetcher).toHaveBeenCalledTimes(1);
});

test('Unexpected success status is rejected without repeating the mutation', async () => {
  const fetcher = vi.fn(async (url: string) => url === '/api/auth/csrf' ? json({ headerName: 'X-CSRF-TOKEN', token: 'safe-token' }) : json({}, 200));
  vi.stubGlobal('fetch', fetcher);
  await expect(new ApiClient().request('/api/admin/users', {})).rejects.toBeInstanceOf(ApiError);
  expect(fetcher).toHaveBeenCalledTimes(2);
});
