import { useEffect, useState } from 'react';
import type { Booking, BookingFilters, HistoryFilters, ModuleOption, Slot, TutorApi } from '../api/tutorApi';
import { tutorApi } from '../api/tutorApi';

const emptyBookingFilters: BookingFilters = { moduleId: '', date: '', status: '' };
const emptyHistoryFilters: HistoryFilters = { status: 'CANCELLED', date: '' };

function messageFor(error: unknown): string {
  if (error instanceof Error && error.message === 'INVALID_STATE') {
    return 'This booking has changed. Refresh the list to see its current status.';
  }
  return 'The request could not be completed. Refresh and try again.';
}

function singaporeTime(value: string): string {
  return new Intl.DateTimeFormat('en-SG', {
    dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Singapore',
  }).format(new Date(value));
}

export function BookingsPage({ api = tutorApi }: { api?: TutorApi }) {
  const [modules, setModules] = useState<ModuleOption[]>([]);
  const [bookingFilters, setBookingFilters] = useState(emptyBookingFilters);
  const [historyFilters, setHistoryFilters] = useState(emptyHistoryFilters);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [history, setHistory] = useState<Slot[]>([]);
  const [selectedBookingId, setSelectedBookingId] = useState('');
  const [loadingBookings, setLoadingBookings] = useState(true);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [error, setError] = useState('');

  async function refreshBookings(filters = bookingFilters) {
    setLoadingBookings(true);
    setError('');
    try {
      setBookings(await api.bookings(filters));
    } catch (failure) {
      setError(messageFor(failure));
    } finally {
      setLoadingBookings(false);
    }
  }

  async function refreshHistory(filters = historyFilters) {
    setLoadingHistory(true);
    try {
      setHistory(await api.history(filters));
    } catch {
      setError('History could not be loaded. Try again.');
    } finally {
      setLoadingHistory(false);
    }
  }

  useEffect(() => {
    void api.modules().then(setModules).catch(() => setError('Modules could not be loaded.'));
    void refreshBookings(emptyBookingFilters);
  // Initial data is loaded once; submit handlers use their current filter state.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api]);

  async function complete(booking: Booking) {
    if (!window.confirm('Confirm that this consultation has finished?')) return;
    setError('');
    try {
      await api.complete(booking.id);
      await refreshBookings();
    } catch (failure) {
      await refreshBookings();
      setError(messageFor(failure));
    }
  }

  return (
    <main className="container py-4">
      <header className="mb-4">
        <p className="text-uppercase text-secondary small mb-1">UniChope · Tutor</p>
        <h1 className="h2">Consultations</h1>
        <p className="text-secondary">Review bookings, mark finished consultations, and check slot history.</p>
      </header>

      {error && <div className="alert alert-danger" role="alert">{error}</div>}

      <section aria-labelledby="bookings-heading" className="card mb-4">
        <div className="card-body">
          <h2 id="bookings-heading" className="h4">Bookings</h2>
          <form className="row g-3 align-items-end mb-3" onSubmit={(event) => {
            event.preventDefault();
            void refreshBookings(bookingFilters);
          }}>
            <div className="col-md-4">
              <label className="form-label" htmlFor="booking-module">Module</label>
              <select id="booking-module" className="form-select" value={bookingFilters.moduleId}
                onChange={(event) => setBookingFilters({ ...bookingFilters, moduleId: event.target.value })}>
                <option value="">All modules</option>
                {modules.map((module) => <option key={module.id} value={module.id}>{module.code} · {module.name}</option>)}
              </select>
            </div>
            <div className="col-md-3">
              <label className="form-label" htmlFor="booking-date">Booking date (Singapore)</label>
              <input id="booking-date" type="date" className="form-control" value={bookingFilters.date}
                onChange={(event) => setBookingFilters({ ...bookingFilters, date: event.target.value })} />
            </div>
            <div className="col-md-3">
              <label className="form-label" htmlFor="booking-status">Status</label>
              <select id="booking-status" className="form-select" value={bookingFilters.status}
                onChange={(event) => setBookingFilters({ ...bookingFilters, status: event.target.value })}>
                <option value="">All statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="CANCELLED">Cancelled</option>
                <option value="COMPLETED">Completed</option>
              </select>
            </div>
            <div className="col-md-2 d-grid"><button className="btn btn-primary" type="submit">Apply filters</button></div>
          </form>
          <button className="btn btn-outline-secondary btn-sm mb-3" type="button" onClick={() => void refreshBookings()}>
            Refresh bookings
          </button>
          {loadingBookings ? <p role="status">Loading bookings…</p> : bookings.length === 0 ?
            <p className="text-secondary">No bookings match these filters.</p> : (
              <div className="list-group">
                {bookings.map((booking) => (
                  <article className="list-group-item" key={booking.id}>
                    <div className="d-flex justify-content-between gap-3 flex-wrap">
                      <div>
                        <div className="form-check mb-2">
                          <input className="form-check-input" type="radio" name="selected-booking" id={`booking-${booking.id}`}
                            checked={selectedBookingId === booking.id} onChange={() => setSelectedBookingId(booking.id)} />
                          <label className="form-check-label fw-semibold" htmlFor={`booking-${booking.id}`}>
                            {booking.moduleCode} · {booking.studentName}
                          </label>
                        </div>
                        <p className="mb-1">{singaporeTime(booking.startTime)} – {singaporeTime(booking.endTime)}</p>
                        <span className="badge text-bg-secondary">{booking.status}</span>
                      </div>
                      {booking.status === 'ACTIVE' && <button type="button" className="btn btn-success align-self-center"
                        onClick={() => void complete(booking)}>Mark completed</button>}
                    </div>
                  </article>
                ))}
              </div>
            )}
        </div>
      </section>

      <section aria-labelledby="history-heading" className="card">
        <div className="card-body">
          <h2 id="history-heading" className="h4">Slot history</h2>
          <form className="row g-3 align-items-end mb-3" onSubmit={(event) => {
            event.preventDefault();
            void refreshHistory(historyFilters);
          }}>
            <div className="col-md-4">
              <label className="form-label" htmlFor="history-status">Slot status</label>
              <select id="history-status" className="form-select" value={historyFilters.status}
                onChange={(event) => setHistoryFilters({ ...historyFilters, status: event.target.value as HistoryFilters['status'] })}>
                <option value="CANCELLED">Cancelled</option>
                <option value="COMPLETED">Completed</option>
              </select>
            </div>
            <div className="col-md-4">
              <label className="form-label" htmlFor="history-date">History date (Singapore)</label>
              <input id="history-date" type="date" className="form-control" value={historyFilters.date}
                onChange={(event) => setHistoryFilters({ ...historyFilters, date: event.target.value })} />
            </div>
            <div className="col-md-4 d-grid"><button className="btn btn-outline-primary" type="submit">Apply history filters</button></div>
          </form>
          {loadingHistory ? <p role="status">Loading slot history…</p> : history.length === 0 ?
            <p className="text-secondary">No slots match these filters.</p> : (
              <ul className="list-group">
                {history.map((slot) => <li className="list-group-item d-flex justify-content-between" key={slot.id}>
                  <span>{slot.moduleCode} · {singaporeTime(slot.startTime)}</span>
                  <span className="badge text-bg-secondary">{slot.status}</span>
                </li>)}
              </ul>
            )}
        </div>
      </section>
    </main>
  );
}
