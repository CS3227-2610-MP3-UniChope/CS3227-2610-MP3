import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiClient, ApiError, PreflightError, StaleRequest, safeError, type Booking, type Module, type Slot } from '../api';
import { Icon, Link, Loading } from '../components';
import { navigate } from '../navigation';
import Confirmation from './Confirmation';
import { Consultation, Timetable } from './Consultations';
import { useRead, validateBooking } from './data';
import { addDays, dateLabel, monday, sgtDate } from './dates';
import { filters } from './filters';
export default function StudentWorkspace({ api, location, signOut }: { api: ApiClient; location: string; signOut: () => void }) {
  const bookings = location.split('?')[0] === '/student/bookings';
  const f = filters(location.split('?')[1] || '', bookings);
  const [revision, setRevision] = useState(0); const [invalid, setInvalid] = useState(false);
  const [selection, setSelection] = useState<Slot | Booking | null>(null); const [pending, setPending] = useState(false);
  const [notice, setNotice] = useState(''); const [error, setError] = useState<unknown>(null); const [retryItem, setRetryItem] = useState<Slot | Booking | null>(null);
  const normalizedLocation = useRef<string | null>(null);
  const [mobile, setMobile] = useState(window.innerWidth < 1024);
  useEffect(() => { const resize = () => setMobile(window.innerWidth < 1024); window.addEventListener('resize', resize); return () => window.removeEventListener('resize', resize); }, []);
  const modules = useRead<Module>(api, '/api/student/modules', 'modules', revision);
  const query = new URLSearchParams();
  if (bookings) { (f.view === 'history' ? (f.status ? [f.status] : ['CANCELLED', 'COMPLETED']) : ['ACTIVE']).forEach(status => query.append('status', status)); }
  else if (f.moduleId) query.set('moduleId', f.moduleId);
  if (f.date) query.set('date', f.date);
  const dataPath = `/api/student/${bookings ? 'bookings' : 'slots'}${query.size ? `?${query}` : ''}`;
  const data = useRead<Booking | Slot>(api, dataPath, bookings ? 'bookings' : 'slots', revision);
  const unknownModule = !bookings && !!f.moduleId && !modules.loading && !modules.error && !modules.data.some(module => module.id === f.moduleId);
  useEffect(() => {
    if (f.invalid || unknownModule) {
      setInvalid(true);
      if (unknownModule) f.clean.delete('moduleId');
      const suffix = f.clean.toString();
      normalizedLocation.current = `${bookings ? '/student/bookings' : '/student/slots'}${suffix ? `?${suffix}` : ''}`;
      navigate(normalizedLocation.current, true);
    } else {
      if (normalizedLocation.current !== location) setInvalid(false);
      normalizedLocation.current = null;
    }
  }, [location, unknownModule]);
  useEffect(() => { setPending(false); setSelection(null); setNotice(''); setError(null); setRetryItem(null); }, [location]);
  // An unmounted workspace or a changed filter must not accept an old mutation completion.
  const [lifecycle] = useState(() => ({ current: 0, active: true }));
  useEffect(() => { lifecycle.active = true; lifecycle.current++; return () => { lifecycle.active = false; lifecycle.current++; }; }, [location, lifecycle]);
  const close = useCallback(() => setSelection(null), []);
  function change(values: Record<string, string>) {
    setInvalid(false);
    const next = new URLSearchParams(window.location.search);
    Object.entries(values).forEach(([key, value]) => value ? next.set(key, value) : next.delete(key));
    navigate(`${bookings ? '/student/bookings' : '/student/slots'}${next.size ? `?${next}` : ''}`);
  }
  function weekChange(week: string) { change({ week, date: f.date && (f.date < week || f.date > addDays(week, 6)) ? '' : f.date }); }
  async function mutate() {
    if (!selection || pending) return;
    const item = selection; const generation = lifecycle.current;
    setPending(true); setError(null); setRetryItem(null);
    try {
      const value = await api.request<unknown>(bookings ? `/api/student/bookings/${item.id}/cancel` : '/api/student/bookings', bookings ? {} : { slotId: item.id });
      validateBooking(value);
      if (!lifecycle.active || lifecycle.current !== generation) return;
      setNotice(bookings ? 'Booking cancelled.' : 'Booking confirmed. You can find the details in My bookings.');
      setRevision(value => value + 1); setSelection(null);
    } catch (failure) {
      if (!lifecycle.active || lifecycle.current !== generation || failure instanceof StaleRequest) return;
      setError(failure); setSelection(null);
      if (failure instanceof PreflightError || (failure instanceof ApiError && failure.code === 'CSRF_INVALID')) setRetryItem(item);
      if (failure instanceof ApiError && [404, 409].includes(failure.status)) setRevision(value => value + 1);
    } finally { if (lifecycle.active && lifecycle.current === generation) setPending(false); }
  }
  useEffect(() => { if (!data.loading && notice && document.activeElement === document.body) document.querySelector<HTMLElement>('h1')?.focus(); }, [data.loading, notice]);
  const moduleDenied = modules.error instanceof ApiError && (modules.error.status === 401 || ['ACCOUNT_INACTIVE', 'FORBIDDEN'].includes(modules.error.code));
  const moduleUnavailable = !!modules.error && !moduleDenied;
  const readError = data.error || (moduleDenied ? modules.error : null);
  const denied = [readError, error].some(e => e instanceof ApiError && e.code === 'FORBIDDEN');
  const slots = bookings ? [] : (data.data as Slot[]).filter(slot => { const date = sgtDate(slot.startTime); return date >= f.week && date <= f.end; });
  return <div className="student-layout"><nav className="workspace-nav" aria-label="Student navigation"><span className="nav-caption">MY WORKSPACE</span><Link href="/student/slots" current={!bookings}><Icon name="calendar" /><span>Find a consultation</span></Link><Link href="/student/bookings" current={bookings}><Icon name="book" /><span>My bookings</span></Link><div className="nav-footnote"><span className="tiny-dot" />All times in Singapore time<span className="nav-timezone">SGT · UTC +08:00</span></div></nav>
    <main id="main-content" tabIndex={-1} className="workspace-main"><div className="page-heading"><div><span className="eyebrow">{bookings ? 'YOUR TIME, ORGANIZED' : 'A GOOD QUESTION DESERVES A CONVERSATION'}</span><h1 tabIndex={-1}>{bookings ? 'My bookings' : 'Find a consultation'}</h1><p className="muted">{bookings ? 'Keep track of your consultations, past and upcoming.' : 'Find the right time to work through it together.'}</p></div><button className="btn btn-outline-secondary refresh-button" onClick={() => setRevision(value => value + 1)}>Refresh</button></div>
      {invalid && <div className="notice error" role="alert">Invalid filters were removed. Please check your filter selections.</div>}
      {notice && <div className="notice success" role="status"><Icon name="check" />{notice}</div>}
      {Boolean(error || readError) && <div className="notice error" role="alert">{safeError(error || readError, !!error)}{denied ? <Link href="/student/slots">Back to home</Link> : retryItem ? <button className="text-button" onClick={() => { setSelection(retryItem); setError(null); }}>Retry</button> : !error ? <button className="text-button" onClick={() => setRevision(value => value + 1)}>Retry</button> : null}</div>}
      {!denied && <>{bookings && <div className="booking-tabs" aria-label="Booking view"><button className={f.view === 'upcoming' ? 'active' : ''} onClick={() => change({ view: 'upcoming', status: '' })}>Upcoming</button><button className={f.view === 'history' ? 'active' : ''} onClick={() => change({ view: 'history' })}>History</button></div>}
        <div className="browse-layout"><aside className="filter-sidebar" aria-label="Consultation filters"><div className="filter-heading"><h2>Refine by</h2><button className="text-button" onClick={() => change({ moduleId: '', date: '', status: '' })}>Reset</button></div>{!bookings && <><label htmlFor="module-filter">Module</label>{modules.loading ? <p role="status" className="muted small">Loading modules…</p> : <select id="module-filter" className="form-select" disabled={moduleUnavailable} value={f.moduleId} onChange={event => change({ moduleId: event.target.value })}><option value="">All modules</option>{moduleUnavailable && f.moduleId && <option value={f.moduleId}>Selected module (filter unavailable)</option>}{modules.data.map(module => <option key={module.id} value={module.id}>{module.code} · {module.name}</option>)}</select>}{moduleUnavailable && <p className="muted small" role="status">Module filter unavailable right now.</p>}</>}
          <label htmlFor="date-filter">{bookings ? 'Consultation date' : 'Specific date'}</label><input className="form-control" id="date-filter" type="date" value={f.date} onChange={event => change(bookings ? { date: event.target.value } : { date: event.target.value, week: event.target.value ? monday(event.target.value) : f.week })} />
          {bookings && f.view === 'history' && <><label htmlFor="status-filter">Status</label><select id="status-filter" className="form-select" value={f.status} onChange={event => change({ status: event.target.value })}><option value="">All history</option><option value="CANCELLED">Cancelled</option><option value="COMPLETED">Completed</option></select></>}
          <div className="filter-tip"><Icon name="clock" /><p>All consultation times are shown in <strong>Singapore time (SGT)</strong>.</p></div></aside>
          <section className="results" aria-label={bookings ? 'Your bookings' : 'Available consultations'}>{!bookings && <div className="week-toolbar"><div><h2>{dateLabel(f.week)} – {dateLabel(f.end, { day: 'numeric', month: 'short', year: 'numeric' })}</h2><span className="muted small">Monday to Sunday · SGT</span></div><div className="week-controls"><button className="btn btn-outline-secondary" aria-label="Previous week" onClick={() => weekChange(addDays(f.week, -7))}>‹</button><button className="btn btn-outline-secondary" onClick={() => weekChange(monday(sgtDate()))}>This week</button><button className="btn btn-outline-secondary" aria-label="Next week" onClick={() => weekChange(addDays(f.week, 7))}>›</button></div></div>}
            {data.loading ? <Loading label={bookings ? 'Loading your bookings' : 'Loading consultations'} /> : readError ? null : bookings ? data.data.length ? <div className="booking-list">{data.data.map(item => <Consultation key={item.id} item={item} booking choose={setSelection} />)}</div> : <div className="empty-state"><Icon name="book" /><h2>No bookings to show</h2><p>{f.view === 'history' ? 'Your completed and cancelled consultations will appear here.' : 'Find a consultation and make time for your next question.'}</p></div> : slots.length ? <Timetable slots={slots} week={f.week} choose={setSelection} mobile={mobile} /> : <div className="empty-state"><Icon name="calendar" /><h2>No consultations available</h2><p>Try another week or adjust your filters. New times may become available.</p></div>}
          </section></div></>}
    </main>{selection && <Confirmation item={selection} cancelling={bookings} pending={pending} close={close} confirm={() => void mutate()} signOut={signOut} />}</div>;
}





