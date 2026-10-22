import { useEffect, useState } from 'react';
import { ApiClient, ApiError, StaleRequest, type Booking, type Module, type Slot } from '../api';
type RecordValue = Record<string, unknown>;
const strings = (x: RecordValue, fields: string[]) => fields.every(key => typeof x[key] === 'string');
const object = (x: unknown): x is RecordValue => !!x && typeof x === 'object' && !Array.isArray(x);
export function validateList<T>(value: unknown, kind: 'modules' | 'slots' | 'bookings'): T[] {
  if (!Array.isArray(value) || !value.every(item => {
    if (!object(item)) return false;
    if (kind === 'modules') return strings(item, ['id', 'code', 'name']);
    const validSlot = strings(item, ['id', 'moduleId', 'moduleCode', 'moduleName', 'tutorName', 'startTime', 'endTime']) && Number.isFinite(Date.parse(item.startTime as string)) && Number.isFinite(Date.parse(item.endTime as string));
    return validSlot && (kind !== 'bookings' || (strings(item, ['slotId', 'status']) && ['ACTIVE', 'CANCELLED', 'COMPLETED'].includes(item.status as string) && typeof item.cancellable === 'boolean'));
  })) throw new ApiError();
  return value as T[];
}
export function validateBooking(value: unknown) { return validateList<Booking>([value], 'bookings')[0]; }
export function useRead<T extends Module | Slot | Booking>(api: ApiClient, path: string, kind: 'modules' | 'slots' | 'bookings', revision = 0) {
  const identity = `${path}:${revision}`;
  const [state, setState] = useState<{ data: T[]; loading: boolean; error: unknown; identity: string }>({ data: [], loading: true, error: null, identity });
  useEffect(() => {
    let active = true; setState({ data: [], loading: true, error: null, identity });
    void api.request<unknown>(path).then(value => validateList<T>(value, kind)).then(data => { if (active) setState({ data, loading: false, error: null, identity }); }).catch(error => { if (active && !(error instanceof StaleRequest)) setState({ data: [], loading: false, error, identity }); });
    return () => { active = false; };
  }, [api, path, kind, revision]);
  return state.identity === identity ? state : { data: [] as T[], loading: true, error: null, identity };
}
