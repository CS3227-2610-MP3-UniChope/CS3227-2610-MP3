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

export default function TutorWorkspace({ api, actor }: { api: ApiClient; actor: Actor }) {
  const [bookings, setBookings] = useState<CompletedBooking[]>([]);
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
  const selectedBookingRef = useRef(bookingId);
  selectedBookingRef.current = bookingId;

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
