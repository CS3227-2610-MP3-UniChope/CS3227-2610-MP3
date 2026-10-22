import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ApiClient, ApiError } from '../api';
import AdminWorkspace from './AdminWorkspace';

const users = [{ id: 't1', name: 'Tutor One', email: 'tutor@example.edu', role: 'TUTOR', active: true }, { id: 'a1', name: 'Admin One', email: 'admin@example.edu', role: 'ADMIN', active: true }];
const modules = [{ id: 'm1', code: 'CS3227', name: 'Software Engineering', active: true }];
const statistics = { capturedAt: '2026-10-23T01:00:00Z', total: 4, completed: 2, cancelled: 1, completionRate: 50, cancellationRate: 25, byModule: [{ id: 'm1', label: 'CS3227', count: 4 }], byTutor: [] };
function client(extra: (path: string, body?: object, protectedRequest?: boolean, method?: string) => unknown = () => undefined) {
  const api = new ApiClient();
  const request = vi.spyOn(api, 'request').mockImplementation(async (path, body, protectedRequest, method) => {
    const result = extra(path, body, protectedRequest, method); if (result !== undefined) return await result;
    if (path === '/api/admin/users' && !body) return users;
    if (path === '/api/admin/modules' && !body) return modules;
    if (path === '/api/admin/assignments' && !body) return [{ tutorId: 't1', moduleId: 'm1' }];
    if (path === '/api/admin/statistics') return statistics;
    if (path.startsWith('/api/admin/bookings')) return [];
    return {};
  });
  return { api, request };
}
describe('Admin workspace', () => {
  it('requires confirmation before deactivation and never offers an Admin target', async () => {
    const { api, request } = client(); render(<AdminWorkspace api={api} location="/admin/users" />);
    await screen.findByText('Tutor One');
    expect(screen.getAllByRole('button', { name: 'Deactivate' })).toHaveLength(1);
    fireEvent.click(screen.getByRole('button', { name: 'Deactivate' }));
    expect(request).not.toHaveBeenCalledWith('/api/admin/users/t1/active', expect.anything(), true, 'PUT');
    fireEvent.click(screen.getByRole('button', { name: 'Confirm change' }));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/api/admin/users/t1/active', { active: false }, true, 'PUT'));
  });
  it('clears the provisioned password on an uncertain failure and does not repeat writes', async () => {
    const { api, request } = client((path, body) => { if (path === '/api/admin/users' && body) return Promise.reject(new ApiError()); });
    render(<AdminWorkspace api={api} location="/admin/users" />);
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'New Student' } });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'new@example.edu' } });
    const password = screen.getByLabelText('Initial password (8–128 characters)');
    fireEvent.change(password, { target: { value: 'Secret123!' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));
    await screen.findByText(/outcome could not be confirmed/);
    expect(password).toHaveValue('');
    expect(request.mock.calls.filter(([path, body]) => path === '/api/admin/users' && !!body)).toHaveLength(1);
  });
  it('sends an empty body with PUT when assigning and no body with DELETE after confirmation', async () => {
    const { api, request } = client(); render(<AdminWorkspace api={api} location="/admin/assignments" />);
    await screen.findByText('Tutor One', { selector: 'td' });
    fireEvent.change(screen.getByLabelText('Tutor'), { target: { value: 't1' } });
    fireEvent.change(screen.getByLabelText('Module'), { target: { value: 'm1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Assign tutor' }));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/api/admin/assignments/t1/m1', {}, true, 'PUT'));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Remove' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Remove' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirm change' }));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/api/admin/assignments/t1/m1', undefined, true, 'DELETE'));
  });
  it('renders fake reports as text alongside their returned snapshot without automatic calls', async () => {
    const draft = '<script>untrusted()</script> Draft';
    const { api, request } = client(path => path === '/api/admin/activity-report' ? { capturedAt: statistics.capturedAt, statistics, draft, requiresReview: true, providerMode: 'fake' } : undefined);
    const { container } = render(<AdminWorkspace api={api} location="/admin/reports" />);
    expect(request.mock.calls.filter(([path]) => path === '/api/admin/activity-report')).toHaveLength(0);
    fireEvent.click(screen.getByRole('button', { name: 'Generate activity report' }));
    await screen.findByText(/Mock AI response/);
    expect(screen.getByText(draft)).toBeInTheDocument(); expect(container.querySelector('script')).toBeNull();
    expect(screen.getByText('50%')).toBeInTheDocument(); expect(screen.getByText('25%')).toBeInTheDocument();
    expect(request).toHaveBeenCalledWith('/api/admin/activity-report', {});
    expect(localStorage.length).toBe(0); expect(sessionStorage.length).toBe(0);
  });
  it('intersects booking filters with SGT date without exposing mutation controls', async () => {
    const { api, request } = client(); render(<AdminWorkspace api={api} location="/admin/bookings" />);
    await waitFor(() => expect(screen.getByLabelText('Module')).toBeEnabled());
    fireEvent.change(screen.getByLabelText('Module'), { target: { value: 'm1' } });
    fireEvent.change(screen.getByLabelText('Tutor'), { target: { value: 't1' } });
    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'ACTIVE' } });
    fireEvent.change(screen.getByLabelText('Consultation date (SGT)'), { target: { value: '2026-10-23' } });
    await waitFor(() => expect(request).toHaveBeenCalledWith('/api/admin/bookings?moduleId=m1&tutorId=t1&status=ACTIVE&date=2026-10-23'));
    expect(screen.queryByRole('button', { name: /cancel booking/i })).toBeNull();
  });
  it('keeps statistics usable when report generation is unavailable', async () => {
    const { api, request } = client(path => path === '/api/admin/activity-report' ? Promise.reject(new ApiError(503, 'AI_UNAVAILABLE')) : undefined);
    const view = render(<AdminWorkspace api={api} location="/admin/reports" />);
    fireEvent.click(screen.getByRole('button', { name: 'Generate activity report' }));
    await screen.findByText(/Statistics and management remain available/);
    expect(request.mock.calls.filter(([path]) => path === '/api/admin/activity-report')).toHaveLength(1);
    view.rerender(<AdminWorkspace api={api} location="/admin/overview" />);
    await screen.findByText('50%');
  });
  it('rejects malformed reports without crashing or displaying an unverified draft', async () => {
    const { api } = client(path => path === '/api/admin/activity-report' ? { statistics, draft: 'Do not display', requiresReview: false, providerMode: 'fake' } : undefined);
    render(<AdminWorkspace api={api} location="/admin/reports" />);
    fireEvent.click(screen.getByRole('button', { name: 'Generate activity report' }));
    await screen.findByRole('alert');
    expect(screen.queryByText('Do not display')).toBeNull();
    expect(screen.getByRole('button', { name: 'Generate activity report' })).toBeEnabled();
  });
  it('ignores a report response after navigating away', async () => {
    let resolve!: (value: unknown) => void;
    const promise = new Promise(value => { resolve = value; });
    const { api } = client(path => path === '/api/admin/activity-report' ? promise : undefined);
    const view = render(<AdminWorkspace api={api} location="/admin/reports" />);
    fireEvent.click(screen.getByRole('button', { name: 'Generate activity report' }));
    view.rerender(<AdminWorkspace api={api} location="/admin/overview" />);
    resolve({ capturedAt: statistics.capturedAt, statistics, draft: 'Old report', requiresReview: true, providerMode: 'fake' });
    await screen.findByText('50%');
    expect(screen.queryByText('Old report')).toBeNull();
    view.rerender(<AdminWorkspace api={api} location="/admin/reports" />);
    expect(screen.queryByText('Old report')).toBeNull();
  });
});
