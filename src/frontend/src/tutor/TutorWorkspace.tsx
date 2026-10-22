import { useEffect, useRef, useState } from 'react';
import { ApiClient, ApiError, StaleRequest, type Actor } from '../api';
import { Loading } from '../components';

type CompletedBooking = {
  bookingId: string;
  slotId: string;
  moduleCode: string;
  startTime: string;
  endTime: string;
};
type BookingRow = CompletedBooking & { status: 'ACTIVE' | 'CANCELLED' | 'COMPLETED' };
type TutorModule = { id: string; code: string; name: string };
type HistorySlot = { id: string; moduleId: string; moduleCode: string; startTime: string; endTime: string; status: 'CANCELLED' | 'COMPLETED' };
type TutorNote = { bookingId: string; content: string | null; version: number; updatedAt: string | null };
type DraftResponse = { bookingId: string; draft: string; requiresReview: boolean; providerMode: 'fake' | 'soc' };

const MAX_CODE_POINTS = 8000;
const codePoints = (value: string) => Array.from(value).length;
const dateLabel = (value: string) => new Intl.DateTimeFormat('en-SG', {
  dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Singapore',
}).format(new Date(value));

function failureMessage(error: unknown) {
  const code = error instanceof ApiError ? error.code : 'UNAVAILABLE';
  const messages: Record<string, string> = {
    AI_BUSY: 'Another draft is already being prepared. Wait a moment and try again.',
    AI_RATE_LIMITED: 'The draft limit has been reached. Try again later.',
    AI_UNAVAILABLE: 'Drafting is temporarily unavailable. Your notes are still here.',
    AI_TIMEOUT: 'Drafting took too long. Your notes are still here; you can continue manually.',
    AI_INVALID_OUTPUT: 'The draft could not be checked safely. Your notes are still here.',
    NOTE_CONFLICT: 'This saved note changed elsewhere. Reload it before saving your edits.',
    INVALID_STATE: 'This consultation is no longer available for notes. Refresh the list.',
    NOT_FOUND: 'This consultation is no longer available. Refresh the list.',
  };
  return messages[code] ?? 'We could not complete that action. Your unsaved text is still here.';
}
function bookingFailureMessage(error: unknown) {
  if (error instanceof ApiError && error.code === 'INVALID_STATE') return 'This booking can no longer be completed. Refresh the list.';
  if (error instanceof ApiError && error.code === 'NOT_FOUND') return 'This booking is no longer available. Refresh the list.';
  return failureMessage(error);
}

export default function TutorWorkspace({ api, actor }: { api: ApiClient; actor: Actor }) {
  const [bookings, setBookings] = useState<CompletedBooking[]>([]);
  const [bookingRows, setBookingRows] = useState<BookingRow[]>([]);
  const [modules, setModules] = useState<TutorModule[]>([]);
  const [bookingModule, setBookingModule] = useState('');
  const [bookingDate, setBookingDate] = useState('');
  const [bookingStatus, setBookingStatus] = useState('');
  const [bookingLoadState, setBookingLoadState] = useState<'loading' | 'ready' | 'failed'>('loading');
  const [bookingError, setBookingError] = useState('');
  const [bookingNotice, setBookingNotice] = useState('');
  const [completingId, setCompletingId] = useState('');
  const [history, setHistory] = useState<HistorySlot[]>([]);
  const [historyStatus, setHistoryStatus] = useState<'CANCELLED' | 'COMPLETED'>('COMPLETED');
  const [historyDate, setHistoryDate] = useState('');
  const [historyLoadState, setHistoryLoadState] = useState<'idle' | 'loading' | 'ready' | 'failed'>('idle');
  const [historyError, setHistoryError] = useState('');
  const [bookingId, setBookingId] = useState('');
  const [listState, setListState] = useState<'loading' | 'ready' | 'failed'>('loading');
  const [listAttempt, setListAttempt] = useState(0);
  const [note, setNote] = useState<TutorNote | null>(null);
  const [noteText, setNoteText] = useState('');
  const [roughNotes, setRoughNotes] = useState('');
  const [draftText, setDraftText] = useState('');
  const [draftBaseline, setDraftBaseline] = useState('');
  const [providerMode, setProviderMode] = useState<'fake' | 'soc' | null>(null);
  const [noteLoading, setNoteLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const draftSequence = useRef(0);
  const bookingFiltersRef = useRef({ moduleId: bookingModule, date: bookingDate, status: bookingStatus });
  const bookingRequestSequence = useRef(0);
  const historyRequestSequence = useRef(0);
  const selectedBookingRef = useRef(bookingId);
  selectedBookingRef.current = bookingId;

  async function loadBookingRows(moduleId = bookingModule, date = bookingDate, status = bookingStatus) {
    const requestSequence = ++bookingRequestSequence.current;
    bookingFiltersRef.current = { moduleId, date, status };
    const query = new URLSearchParams();
    if (moduleId) query.set('moduleId', moduleId);
    if (date) query.set('date', date);
    if (status) query.set('status', status);
    setBookingLoadState('loading'); setBookingError(''); setBookingNotice('');
    try {
      const rows = await api.request<BookingRow[]>(`/api/tutor/bookings${query.size ? `?${query}` : ''}`);
      if (requestSequence !== bookingRequestSequence.current) return false;
      setBookingRows(rows); setBookingLoadState('ready'); return true;
    } catch (cause) {
      if (requestSequence !== bookingRequestSequence.current) return false;
      if (!(cause instanceof StaleRequest)) setBookingError(bookingFailureMessage(cause));
      setBookingLoadState('failed'); return false;
    }
  }

  async function loadHistory(status = historyStatus, date = historyDate) {
    const requestSequence = ++historyRequestSequence.current;
    const query = new URLSearchParams({ status });
    if (date) query.set('date', date);
    setHistoryLoadState('loading'); setHistoryError('');
    try {
      const rows = await api.request<HistorySlot[]>(`/api/tutor/history/slots?${query}`);
      if (requestSequence !== historyRequestSequence.current) return;
      setHistory(rows);
      setHistoryLoadState('ready');
    } catch (cause) {
      if (requestSequence !== historyRequestSequence.current) return;
      if (!(cause instanceof StaleRequest)) setHistoryError(failureMessage(cause));
      setHistoryLoadState('failed');
    }
  }

  async function completeBooking(item: BookingRow) {
    if (item.status !== 'ACTIVE' || completingId) return;
    if (!window.confirm('Mark this consultation as completed? This updates the booking and slot together.')) return;
    setCompletingId(item.bookingId); setBookingError(''); setBookingNotice('');
    try {
      await api.request(`/api/tutor/bookings/${item.bookingId}/complete`, {});
      const filters = bookingFiltersRef.current;
      const refreshed = await loadBookingRows(filters.moduleId, filters.date, filters.status);
      if (refreshed) setBookingNotice('Consultation marked completed.');
    } catch (cause) {
      if (!(cause instanceof StaleRequest)) {
        const message = bookingFailureMessage(cause);
        const filters = bookingFiltersRef.current;
        await loadBookingRows(filters.moduleId, filters.date, filters.status);
        setBookingError(message);
      }
    } finally { setCompletingId(''); }
  }

  useEffect(() => {
    let current = true;
    void api.request<TutorModule[]>('/api/tutor/modules').then(value => { if (current) setModules(value); }).catch(() => {});
    void loadBookingRows();
    return () => { current = false; };
  }, [api, actor.id]);

  useEffect(() => {
    let current = true;
    setListState('loading');
    void api.request<CompletedBooking[]>('/api/tutor/bookings?status=COMPLETED').then(result => {
      if (!current) return;
      setBookings(result);
      setBookingId(result[0]?.bookingId ?? '');
      setListState('ready');
    }).catch(() => { if (current) setListState('failed'); });
    return () => { current = false; draftSequence.current++; };
  }, [api, actor.id, listAttempt]);

  useEffect(() => {
    if (!bookingId) { setNote(null); setNoteText(''); return; }
    let current = true;
    setNoteLoading(true); setSaving(false); setGenerating(false); setNote(null); setNoteText(''); setRoughNotes('');
    setDraftText(''); setDraftBaseline(''); setProviderMode(null); setError(''); setNotice('');
    void api.request<TutorNote>(`/api/tutor/bookings/${bookingId}/note`).then(value => {
      if (!current) return;
      setNote(value); setNoteText(value.content ?? '');
    }).catch(cause => {
      if (!current || cause instanceof StaleRequest) return;
      setError(failureMessage(cause));
    }).finally(() => {
      if (current) setNoteLoading(false);
    });
    return () => { current = false; };
  }, [api, bookingId]);

  useEffect(() => {
    const noteDirty = note !== null && noteText !== (note.content ?? '');
    const draftDirty = providerMode !== null && draftText !== draftBaseline;
    if (!roughNotes && !noteDirty && !draftDirty) return;
    function warnBeforeLeave(event: BeforeUnloadEvent) {
      event.preventDefault();
      event.returnValue = '';
    }
    window.addEventListener('beforeunload', warnBeforeLeave);
    return () => window.removeEventListener('beforeunload', warnBeforeLeave);
  }, [note, noteText, providerMode, draftText, draftBaseline, roughNotes]);

  function selectBooking(nextId: string) {
    if (nextId === bookingId) return;
    const noteDirty = note !== null && noteText !== (note.content ?? '');
    const hasWork = roughNotes.length > 0 || draftText.length > 0 || noteDirty;
    if (hasWork && !window.confirm('Discard the unsaved note and draft before changing consultation?')) return;
    draftSequence.current++;
    setBookingId(nextId);
  }

  async function generate() {
    if (!bookingId || !roughNotes.trim() || codePoints(roughNotes) > MAX_CODE_POINTS || generating) return;
    if (draftText !== draftBaseline && !window.confirm('Replace your edited draft with a new generated draft?')) return;
    const sequence = ++draftSequence.current;
    const requestedBooking = bookingId;
    setGenerating(true); setError(''); setNotice('');
    try {
      const result = await api.request<DraftResponse>(`/api/tutor/bookings/${requestedBooking}/note-draft`, { roughNotes });
      if (sequence !== draftSequence.current || requestedBooking !== selectedBookingRef.current || result.bookingId !== requestedBooking) return;
      setDraftText(result.draft); setDraftBaseline(result.draft); setProviderMode(result.providerMode);
      setNotice('Draft ready for your review. It has not been saved.');
    } catch (cause) {
      if (sequence === draftSequence.current && selectedBookingRef.current === requestedBooking && !(cause instanceof StaleRequest)) setError(failureMessage(cause));
    } finally {
      if (sequence === draftSequence.current && selectedBookingRef.current === requestedBooking) setGenerating(false);
    }
  }

  function useDraft() {
    if (!draftText) return;
    const noteDirty = note !== null && noteText !== (note.content ?? '');
    if (noteDirty && !window.confirm('Replace the text in the note editor with this reviewed draft?')) return;
    setNoteText(draftText); setNotice('Draft copied into the note editor. Choose Save note to store it.'); setError('');
  }

  async function saveNote() {
    if (!bookingId || !note || !noteText.trim() || codePoints(noteText) > MAX_CODE_POINTS || saving) return;
    const requestedBooking = bookingId;
    setSaving(true); setError(''); setNotice('');
    try {
      const saved = await api.put<TutorNote>(`/api/tutor/bookings/${bookingId}/note`, {
        content: noteText, expectedVersion: note.version,
      });
      if (saved.bookingId !== requestedBooking || selectedBookingRef.current !== requestedBooking) return;
      setNote(saved); setNoteText(saved.content ?? ''); setNotice('Note saved.');
    } catch (cause) { if (selectedBookingRef.current === requestedBooking && !(cause instanceof StaleRequest)) setError(failureMessage(cause)); }
    finally { if (selectedBookingRef.current === requestedBooking) setSaving(false); }
  }

  async function reloadNote() {
    if (!bookingId || noteLoading) return;
    if (note && noteText !== (note.content ?? '') && !window.confirm('Discard your unsaved note edits and reload the saved note?')) return;
    const requestedBooking = bookingId;
    setNoteLoading(true); setError(''); setNotice('');
    try {
      const loaded = await api.request<TutorNote>(`/api/tutor/bookings/${requestedBooking}/note`);
      if (loaded.bookingId !== requestedBooking || selectedBookingRef.current !== requestedBooking) return;
      setNote(loaded); setNoteText(loaded.content ?? ''); setNotice('Saved note reloaded.');
    } catch (cause) { if (selectedBookingRef.current === requestedBooking && !(cause instanceof StaleRequest)) setError(failureMessage(cause)); }
    finally { if (selectedBookingRef.current === requestedBooking) setNoteLoading(false); }
  }

  const selected = bookings.find(item => item.bookingId === bookingId);
  return <main id="main-content" className="tutor-workspace">
    <div className="tutor-page-heading"><span className="eyebrow">TUTOR WORKSPACE</span><h1>Consultation notes</h1><p>Review completed consultations, prepare a draft, and save your notes when ready.</p></div>
    <section className="tutor-panel" aria-labelledby="tutor-bookings-heading">
      <h2 id="tutor-bookings-heading">Bookings</h2>
      <form className="tutor-list-filters" onSubmit={event => { event.preventDefault(); void loadBookingRows(bookingModule, bookingDate, bookingStatus); }}>
        <label>Module<select className="form-select" value={bookingModule} onChange={event => setBookingModule(event.target.value)}>
          <option value="">All modules</option>{modules.map(item => <option key={item.id} value={item.id}>{item.code}</option>)}
        </select></label>
        <label>Start date<input className="form-control" type="date" value={bookingDate} onChange={event => setBookingDate(event.target.value)} /></label>
        <label>Status<select className="form-select" value={bookingStatus} onChange={event => setBookingStatus(event.target.value)}>
          <option value="">All statuses</option><option value="ACTIVE">Active</option><option value="CANCELLED">Cancelled</option><option value="COMPLETED">Completed</option>
        </select></label>
        <button className="btn btn-outline-secondary" type="submit">Apply filters</button>
      </form>
      {bookingError && <p role="alert" className="tutor-alert">{bookingError}</p>}
      {bookingNotice && <p role="status" className="tutor-notice">{bookingNotice}</p>}
      {bookingLoadState === 'loading' && <Loading label="Loading Tutor bookings" />}
      {bookingLoadState === 'failed' && <button className="btn btn-outline-secondary" onClick={() => void loadBookingRows()}>Retry bookings</button>}
      {bookingLoadState === 'ready' && bookingRows.length === 0 && <p>No bookings match these filters.</p>}
      {bookingLoadState === 'ready' && bookingRows.length > 0 && <div className="table-responsive"><table className="table">
        <thead><tr><th>Module</th><th>Consultation time (Singapore)</th><th>Status</th><th>Action</th></tr></thead>
        <tbody>{bookingRows.map(item => <tr key={item.bookingId}>
          <td>{item.moduleCode}</td><td>{dateLabel(item.startTime)} – {dateLabel(item.endTime)}</td><td>{item.status}</td>
          <td>{item.status === 'ACTIVE' && <button className="btn btn-sm btn-outline-secondary" disabled={completingId !== ''} onClick={() => void completeBooking(item)}>
            {completingId === item.bookingId ? 'Completing…' : 'Mark complete'}
          </button>}</td>
        </tr>)}</tbody>
      </table></div>}
    </section>
    <section className="tutor-panel" aria-labelledby="tutor-history-heading">
      <h2 id="tutor-history-heading">Slot history</h2>
      <form className="tutor-list-filters" onSubmit={event => { event.preventDefault(); void loadHistory(historyStatus, historyDate); }}>
        <label>Status<select className="form-select" value={historyStatus} onChange={event => setHistoryStatus(event.target.value as 'CANCELLED' | 'COMPLETED')}>
          <option value="CANCELLED">Cancelled</option><option value="COMPLETED">Completed</option>
        </select></label>
        <label>Start date<input className="form-control" type="date" value={historyDate} onChange={event => setHistoryDate(event.target.value)} /></label>
        <button className="btn btn-outline-secondary" type="submit">Load history</button>
      </form>
      {historyError && <p role="alert" className="tutor-alert">{historyError}</p>}
      {historyLoadState === 'idle' && <p className="muted">Choose a status and load your cancelled or completed slots.</p>}
      {historyLoadState === 'loading' && <Loading label="Loading slot history" />}
      {historyLoadState === 'failed' && <button className="btn btn-outline-secondary" onClick={() => void loadHistory()}>Retry history</button>}
      {historyLoadState === 'ready' && history.length === 0 && <p>No historical slots match these filters.</p>}
      {historyLoadState === 'ready' && history.length > 0 && <div className="table-responsive"><table className="table">
        <thead><tr><th>Module</th><th>Consultation time (Singapore)</th><th>Status</th></tr></thead>
        <tbody>{history.map(item => <tr key={item.id}><td>{item.moduleCode}</td><td>{dateLabel(item.startTime)} – {dateLabel(item.endTime)}</td><td>{item.status}</td></tr>)}</tbody>
      </table></div>}
    </section>
    {listState === 'loading' && <Loading label="Loading completed consultations" />}
    {listState === 'failed' && <section className="tutor-panel"><p role="alert">We could not load your completed consultations.</p><button className="btn btn-outline-secondary" onClick={() => setListAttempt(value => value + 1)}>Try again</button></section>}
    {listState === 'ready' && bookings.length === 0 && <section className="tutor-panel"><h2>No completed consultations yet</h2><p>Completed consultations will appear here for note taking.</p></section>}
    {listState === 'ready' && bookings.length > 0 && <>
      <section className="tutor-panel tutor-booking-picker">
        <label htmlFor="tutor-booking">Completed consultation</label>
        <select id="tutor-booking" className="form-select" value={bookingId} onChange={event => selectBooking(event.target.value)}>
          {bookings.map(item => <option key={item.bookingId} value={item.bookingId}>{item.moduleCode} · {dateLabel(item.startTime)}</option>)}
        </select>
        {selected && <p className="muted">{dateLabel(selected.startTime)} – {dateLabel(selected.endTime)} (Singapore time)</p>}
      </section>
      {error && <p className="tutor-alert" role="alert">{error}</p>}
      {notice && <p className="tutor-notice" role="status">{notice}</p>}
      <div className="tutor-notes-grid">
        <section className="tutor-panel">
          <h2>Saved consultation note</h2>
          <label htmlFor="saved-note">Tutor-only note</label>
          <textarea id="saved-note" className="form-control tutor-note-area" value={noteText} maxLength={16000}
            disabled={noteLoading || !note} onChange={event => setNoteText(event.target.value)} />
          <div className="tutor-note-actions"><span>{codePoints(noteText)} / {MAX_CODE_POINTS} characters{note && ` · version ${note.version}`}</span>
            <button className="btn btn-outline-secondary" onClick={() => void reloadNote()} disabled={noteLoading}>Reload saved note</button>
            <button className="btn btn-primary" onClick={() => void saveNote()} disabled={!note || noteLoading || saving || !noteText.trim() || codePoints(noteText) > MAX_CODE_POINTS || noteText === (note.content ?? '')}>{saving ? 'Saving…' : 'Save note'}</button>
          </div>
          {note?.updatedAt && <p className="muted">Saved {dateLabel(note.updatedAt)}</p>}
        </section>
        <section className="tutor-panel">
          <h2>Prepare a draft</h2>
          <p className="muted">Enter rough notes. The assistant returns a suggestion for you to review; generation never saves it.</p>
          <label htmlFor="rough-notes">Rough notes</label>
          <textarea id="rough-notes" className="form-control tutor-note-area tutor-rough-area" value={roughNotes}
            disabled={generating} onChange={event => setRoughNotes(event.target.value)} />
          <div className="tutor-note-actions"><span>{codePoints(roughNotes)} / {MAX_CODE_POINTS} characters</span>
            <button className="btn btn-primary" onClick={() => void generate()} disabled={generating || !roughNotes.trim() || codePoints(roughNotes) > MAX_CODE_POINTS}>{generating ? 'Preparing draft…' : providerMode ? 'Generate again' : 'Generate draft'}</button>
          </div>
          {providerMode && <div className="tutor-draft">
            <div className="tutor-draft-heading"><h3>Draft for review</h3>{providerMode === 'fake' && <span className="badge text-bg-secondary">FAKE RESULT</span>}</div>
            {providerMode === 'fake' && <p className="muted">This deterministic local result is for testing, not an SoC response.</p>}
            <label htmlFor="draft-text">Editable draft</label>
            <textarea id="draft-text" className="form-control tutor-note-area" value={draftText} onChange={event => setDraftText(event.target.value)} />
            <button className="btn btn-outline-primary" onClick={useDraft} disabled={!draftText}>Use reviewed draft</button>
          </div>}
        </section>
      </div>
    </>}
  </main>;
}
