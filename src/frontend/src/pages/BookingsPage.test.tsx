import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Booking, Slot, TutorApi } from '../api/tutorApi';
import { BookingsPage } from './BookingsPage';

const booking: Booking = {
  id: 'booking-1', slotId: 'slot-1', moduleId: 'module-1', moduleCode: 'CS3227',
  studentName: 'Student One', startTime: '2026-10-21T02:00:00Z', endTime: '2026-10-21T03:00:00Z', status: 'ACTIVE',
};
const completed: Booking = { ...booking, status: 'COMPLETED' };
const slot: Slot = {
  id: 'history-1', moduleId: 'module-1', moduleCode: 'CS3227',
  startTime: '2026-10-20T02:00:00Z', endTime: '2026-10-20T03:00:00Z', status: 'CANCELLED',
};

function fakeApi(overrides: Partial<TutorApi> = {}): TutorApi {
  return {
    modules: vi.fn().mockResolvedValue([{ id: 'module-1', code: 'CS3227', name: 'Software Engineering' }]),
    bookings: vi.fn().mockResolvedValue([booking]),
    history: vi.fn().mockResolvedValue([slot]),
    complete: vi.fn().mockResolvedValue(completed),
    ...overrides,
  };
}

describe('Tutor bookings page', () => {
  it('does not send completion when the tutor cancels the confirmation', async () => {
    const api = fakeApi();
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const user = userEvent.setup();
    render(<BookingsPage api={api} />);

    await user.click(await screen.findByRole('button', { name: 'Mark completed' }));

    expect(api.complete).not.toHaveBeenCalled();
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
  });

  it('confirms completion then refreshes the booking state', async () => {
    const api = fakeApi({ bookings: vi.fn().mockResolvedValueOnce([booking]).mockResolvedValueOnce([completed]) });
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const user = userEvent.setup();
    render(<BookingsPage api={api} />);

    await user.click(await screen.findByRole('button', { name: 'Mark completed' }));

    await waitFor(() => expect(api.complete).toHaveBeenCalledWith(booking.id));
    expect(await screen.findByText('COMPLETED')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Mark completed' })).not.toBeInTheDocument();
    expect(api.bookings).toHaveBeenCalledTimes(2);
  });

  it('keeps the selected booking and booking filters when history filters change', async () => {
    const api = fakeApi();
    const user = userEvent.setup();
    render(<BookingsPage api={api} />);

    const selected = await screen.findByRole('radio', { name: /CS3227 · Student One/ });
    await user.click(selected);
    await user.selectOptions(screen.getByLabelText('Status'), 'ACTIVE');
    await user.selectOptions(screen.getByLabelText('Slot status'), 'COMPLETED');
    await user.click(screen.getByRole('button', { name: 'Apply history filters' }));

    expect(selected).toBeChecked();
    expect(screen.getByLabelText('Status')).toHaveValue('ACTIVE');
    expect(api.bookings).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(api.history).toHaveBeenLastCalledWith({ status: 'COMPLETED', date: '' }));
  });

  it('submits the independent module, date, and status booking filters', async () => {
    const api = fakeApi();
    const user = userEvent.setup();
    render(<BookingsPage api={api} />);

    await screen.findByRole('radio', { name: /CS3227 · Student One/ });
    await user.selectOptions(screen.getByLabelText('Module'), 'module-1');
    await user.type(screen.getByLabelText('Booking date (Singapore)'), '2026-10-21');
    await user.selectOptions(screen.getByLabelText('Status'), 'ACTIVE');
    await user.click(screen.getByRole('button', { name: 'Apply filters' }));

    await waitFor(() => expect(api.bookings).toHaveBeenLastCalledWith({
      moduleId: 'module-1', date: '2026-10-21', status: 'ACTIVE',
    }));
  });

  it('shows a safe completion error and keeps refresh available after conflict', async () => {
    const api = fakeApi({
      bookings: vi.fn().mockResolvedValueOnce([booking]).mockResolvedValueOnce([completed]),
      complete: vi.fn().mockRejectedValue(new Error('INVALID_STATE')),
    });
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const user = userEvent.setup();
    render(<BookingsPage api={api} />);

    await user.click(await screen.findByRole('button', { name: 'Mark completed' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('This booking has changed. Refresh the list');
    expect(screen.getByRole('button', { name: 'Refresh bookings' })).toBeEnabled();
    expect(screen.getByText('COMPLETED')).toBeInTheDocument();
  });
});
