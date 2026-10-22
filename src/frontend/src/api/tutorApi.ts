export type BookingStatus = 'ACTIVE' | 'CANCELLED' | 'COMPLETED';
export type SlotStatus = 'CANCELLED' | 'COMPLETED';

export interface Booking {
  id: string;
  slotId: string;
  moduleId: string;
  moduleCode: string;
  studentName: string;
  startTime: string;
  endTime: string;
  status: BookingStatus;
}

export interface Slot {
  id: string;
  moduleId: string;
  moduleCode: string;
  startTime: string;
  endTime: string;
  status: SlotStatus;
}

export interface ModuleOption {
  id: string;
  code: string;
  name: string;
}

export interface BookingFilters {
  moduleId: string;
  date: string;
  status: string;
}

export interface HistoryFilters {
  status: SlotStatus;
  date: string;
}

export interface TutorApi {
  modules(): Promise<ModuleOption[]>;
  bookings(filters: BookingFilters): Promise<Booking[]>;
  history(filters: HistoryFilters): Promise<Slot[]>;
  complete(bookingId: string): Promise<Booking>;
}

async function json<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { credentials: 'include', ...init });
  if (!response.ok) {
    const error = await response.json().catch(() => ({ code: 'REQUEST_FAILED' }));
    throw new Error(typeof error.code === 'string' ? error.code : 'REQUEST_FAILED');
  }
  return response.json() as Promise<T>;
}

async function csrfHeader(): Promise<Record<string, string>> {
  const token = await json<{ headerName: string; token: string }>('/api/auth/csrf');
  return { [token.headerName]: token.token };
}

export const tutorApi: TutorApi = {
  modules: () => json('/api/tutor/modules'),
  bookings: (filters) => {
    const query = new URLSearchParams();
    if (filters.moduleId) query.set('moduleId', filters.moduleId);
    if (filters.date) query.set('date', filters.date);
    if (filters.status) query.set('status', filters.status);
    return json(`/api/tutor/bookings${query.size ? `?${query}` : ''}`);
  },
  history: (filters) => {
    const query = new URLSearchParams({ status: filters.status });
    if (filters.date) query.set('date', filters.date);
    return json(`/api/tutor/history/slots?${query}`);
  },
  async complete(bookingId) {
    const headers = await csrfHeader();
    return json(`/api/tutor/bookings/${encodeURIComponent(bookingId)}/complete`, {
      method: 'POST',
      headers: { ...headers, 'Content-Type': 'application/json' },
      body: '{}',
    });
  },
};
