import { useEffect, useState } from 'react';
import { ApiClient, ApiError, PreflightError, StaleRequest } from '../api';

export type User = { id: string; name: string; email: string; role: string; active: boolean };
export type AdminModule = { id: string; code: string; name: string; active: boolean };
export type Assignment = { tutorId: string; moduleId: string };
export type Booking = { id: string; studentId: string; studentName: string; tutorId: string; tutorName: string; moduleId: string; moduleCode: string; startTime: string; endTime: string; status: string };
export type Statistics = { capturedAt: string; total: number; completed: number; cancelled: number; completionRate: number; cancellationRate: number; byModule: { id: string | null; label: string; count: number }[]; byTutor: { id: string | null; label: string; count: number }[] };
export type Report = { capturedAt: string; statistics: Statistics; draft: string; requiresReview: boolean; providerMode: string };
export function validateStatistics(value: unknown): Statistics {
  if (!value || typeof value !== 'object') throw new ApiError();
  const s = value as Statistics;
  if (typeof s.capturedAt !== 'string' || !Number.isFinite(Date.parse(s.capturedAt)) ||
      ![s.total, s.completed, s.cancelled].every(n => Number.isSafeInteger(n) && n >= 0) ||
      ![s.completionRate, s.cancellationRate].every(n => typeof n === 'number' && Number.isFinite(n) && n >= 0 && n <= 100) ||
      ![s.byModule, s.byTutor].every(groups => Array.isArray(groups) && groups.every(g => g && (g.id === null || typeof g.id === 'string') && typeof g.label === 'string' && Number.isSafeInteger(g.count) && g.count >= 0))) throw new ApiError();
  return s;
}
export function validateReport(value: unknown): Report {
  if (!value || typeof value !== 'object') throw new ApiError();
  const r = value as Report;
  if (typeof r.capturedAt !== 'string' || !Number.isFinite(Date.parse(r.capturedAt)) || typeof r.draft !== 'string' || !r.draft.trim() || r.requiresReview !== true || !['fake', 'soc', 'none'].includes(r.providerMode)) throw new ApiError();
  validateStatistics(r.statistics);
  return r;
}
function validateList(path: string, value: unknown) {
  if (!Array.isArray(value)) throw new ApiError();
  const strings = (v: Record<string, unknown>, keys: string[]) => keys.every(k => typeof v[k] === 'string');
  for (const raw of value) {
    if (!raw || typeof raw !== 'object') throw new ApiError();
    const v = raw as Record<string, unknown>;
    if (path.startsWith('/api/admin/users') && (!strings(v, ['id', 'name', 'email', 'role']) || !['STUDENT', 'TUTOR', 'ADMIN'].includes(v.role as string) || typeof v.active !== 'boolean')) throw new ApiError();
    if (path.startsWith('/api/admin/modules') && (!strings(v, ['id', 'code', 'name']) || typeof v.active !== 'boolean')) throw new ApiError();
    if (path.startsWith('/api/admin/assignments') && !strings(v, ['tutorId', 'moduleId'])) throw new ApiError();
    if (path.startsWith('/api/admin/bookings') && (!strings(v, ['id', 'studentId', 'studentName', 'tutorId', 'tutorName', 'moduleId', 'moduleCode', 'startTime', 'endTime', 'status']) || !Number.isFinite(Date.parse(v.startTime as string)) || !Number.isFinite(Date.parse(v.endTime as string)) || !['ACTIVE', 'CANCELLED', 'COMPLETED'].includes(v.status as string))) throw new ApiError();
  }
}
export function useAdminRead<T>(api: ApiClient, path: string, revision: number): { data?: T; loading: boolean; error?: unknown } {
  const identity = `${path}:${revision}`;
  const [state, setState] = useState<{ identity: string; data?: T; loading: boolean; error?: unknown }>({ identity, loading: true });
  useEffect(() => {
    let active = true; setState({ identity, loading: true });
    void api.request<T>(path).then(data => { if (path === '/api/admin/statistics') validateStatistics(data); else validateList(path, data); if (active) setState({ identity, data, loading: false }); }).catch(error => { if (active && !(error instanceof StaleRequest)) setState({ identity, loading: false, error }); });
    return () => { active = false; };
  }, [api, path, revision]);
  return state.identity === identity ? state : { loading: true };
}
export function adminError(error: unknown, mutation = false) {
  if (error instanceof PreflightError) return 'Nothing was sent. Please try again.';
  const e = error instanceof ApiError ? error : new ApiError();
  const known: Record<string, string> = {
    VALIDATION_FAILED: 'Check your entries and try again.', DUPLICATE_RECORD: 'An account or module with these details already exists.', EMAIL_IN_USE: 'This email address is already registered.',
    INVALID_STATE: 'This change is blocked by active bookings, future available slots, or an inactive account or module. Refresh and check the records.',
    FORBIDDEN: 'You do not have permission to perform this action.', NOT_FOUND: 'This record is no longer available. Refresh the list.',
    CSRF_INVALID: 'Your security token expired. Submit again to use a fresh token.', AI_BUSY: 'A report is already being generated. Wait before requesting another.',
    AI_UNAVAILABLE: 'The activity report service is unavailable. Statistics and management remain available.', AI_TIMEOUT: 'The report request timed out. Wait before requesting another report.',
    AI_QUOTA_EXCEEDED: 'The report generation limit has been reached. Try again later.', AI_INVALID_OUTPUT: 'The report could not be validated. No draft is shown.',
  };
  if (known[e.code]) return known[e.code];
  if (e.status === 429) return 'The request limit has been reached. Please wait before trying again.';
  return mutation ? 'The outcome could not be confirmed. Refresh and check the records before submitting again.' : 'The service could not be reached. Please try again.';
}
export const dateTime = (value: string) => new Intl.DateTimeFormat('en-SG', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Singapore' }).format(new Date(value));
