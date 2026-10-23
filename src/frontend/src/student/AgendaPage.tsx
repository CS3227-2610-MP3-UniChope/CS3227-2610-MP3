import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiClient, ApiError, StaleRequest, safeError, type Booking } from '../api';
import { Icon, Link, Loading } from '../components';
import { navigate } from '../navigation';
import AgendaConfirmation from './AgendaConfirmation';
import { validateBooking } from './data';
import { dateLabel, sgtDate, timeRange } from './dates';

type Draft = { bookingId: string; agenda: string; requiresReview: true; providerMode: 'fake' | 'soc' };
const validText = (value: string) => value.trim().length > 0 && [...value].length <= 4000 && !/[\uD800-\uDBFF](?![\uDC00-\uDFFF])|(?<![\uD800-\uDBFF])[\uDC00-\uDFFF]/u.test(value);
function agendaError(error: unknown) {
  if (!(error instanceof ApiError)) return 'Unable to generate an agenda. Please try again.';
  const messages: Record<string, string> = {
    AI_TIMEOUT: 'AI generation timed out. Please try again.', AI_INVALID_OUTPUT: 'AI returned an invalid agenda. Please try again.',
    AI_UNAVAILABLE: 'AI is currently unavailable. You can still prepare your questions manually.',
    AI_BUSY: 'An AI generation is already in progress.', AI_RATE_LIMITED: 'The AI generation limit has been reached.',
    INVALID_STATE: 'This booking is no longer available for preparation.', NOT_FOUND: 'This booking could not be found.',
  };
  const retry = ['AI_BUSY', 'AI_RATE_LIMITED'].includes(error.code) && /^[1-9]\d*$/.test(error.retryAfter) ? ` Try again in ${error.retryAfter} seconds.` : '';
  return (messages[error.code] || safeError(error)) + retry;
}

export default function AgendaPage({ api, bookingId }: { api: ApiClient; bookingId: string }) {
  const [booking, setBooking] = useState<Booking | null>(null), [loading, setLoading] = useState(true);
  const [questions, setQuestions] = useState(''), [draft, setDraft] = useState(''), [original, setOriginal] = useState('');
  const [mode, setMode] = useState(''), [pending, setPending] = useState(false), [error, setError] = useState(''), [copyStatus, setCopyStatus] = useState('');
  const [dialog, setDialog] = useState<'regenerate' | string | null>(null);
  const lifecycle = useRef({ active: true, sequence: 0 });
  const draftRef = useRef<HTMLTextAreaElement>(null);
  const leaveAction = useRef<(() => void) | null>(null);
  const close = useCallback(() => setDialog(null), []);
  const dirty = !!questions || draft !== original;
  useEffect(() => {
    const current = lifecycle.current; current.active = true; const sequence = ++current.sequence;
    void api.request<unknown>(`/api/student/bookings/${bookingId}`).then(validateBooking).then(value => {
      if (!current.active || current.sequence !== sequence) return;
      if (value.id !== bookingId || value.status !== 'ACTIVE' || Date.parse(value.endTime) <= Date.now()) throw new ApiError(409, 'INVALID_STATE');
      setBooking(value);
    }).catch(failure => { if (current.active && current.sequence === sequence && !(failure instanceof StaleRequest)) setError(agendaError(failure)); })
      .finally(() => { if (current.active && current.sequence === sequence) setLoading(false); });
    return () => { current.active = false; current.sequence++; };
  }, [api, bookingId]);
  useEffect(() => {
    if (!dirty) return;
    const unload = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = ''; };
    const leaving = (event: Event) => {
      const detail = (event as CustomEvent<{ path: string; proceed?: () => void }>).detail;
      if (!detail.proceed && detail.path === window.location.pathname + window.location.search) return;
      leaveAction.current = detail.proceed || (() => navigate(detail.path, false, true));
      event.preventDefault(); setDialog(detail.path);
    };
    window.addEventListener('beforeunload', unload); window.addEventListener('unichope:navigate', leaving);
    return () => { window.removeEventListener('beforeunload', unload); window.removeEventListener('unichope:navigate', leaving); };
  }, [dirty]);
  async function generate() {
    if (pending) return;
    if (!validText(questions)) { setError('Enter questions using 1 to 4,000 Unicode characters.'); return; }
    setDialog(null); setPending(true); setError(''); setCopyStatus('');
    const current = lifecycle.current, sequence = ++current.sequence;
    try {
      const value = await api.request<Draft>(`/api/student/bookings/${bookingId}/agenda`, { questions });
      if (!current.active || current.sequence !== sequence) return;
      if (!value || value.bookingId !== bookingId || typeof value.agenda !== 'string' || !validText(value.agenda) || value.requiresReview !== true || !['fake', 'soc'].includes(value.providerMode)) throw new ApiError(502, 'AI_INVALID_OUTPUT');
      setDraft(value.agenda); setOriginal(value.agenda); setMode(value.providerMode);
    } catch (failure) { if (current.active && current.sequence === sequence && !(failure instanceof StaleRequest)) setError(agendaError(failure)); }
    finally { if (current.active && current.sequence === sequence) setPending(false); }
  }
  async function copy() {
    try { await navigator.clipboard.writeText(draft); if (lifecycle.current.active) setCopyStatus('Agenda copied to clipboard.'); }
    catch { if (lifecycle.current.active) { draftRef.current?.focus(); draftRef.current?.select(); setCopyStatus('Select the agenda and copy it manually with Ctrl+C or Command+C.'); } }
  }
  return <div className="student-layout"><nav className="workspace-nav" aria-label="Student navigation"><span className="nav-caption">MY WORKSPACE</span><Link href="/student/slots"><Icon name="calendar" /><span>Find a consultation</span></Link><Link href="/student/bookings"><Icon name="book" /><span>My bookings</span></Link><div className="nav-footnote">All times in Singapore time<span className="nav-timezone">SGT · UTC +08:00</span></div></nav>
    <main id="main-content" tabIndex={-1} className="workspace-main"><div className="page-heading"><div><span className="eyebrow">MAKE THE MOST OF YOUR TIME</span><h1>Prepare agenda</h1><p className="muted">Turn your topics and questions into a plan for your consultation.</p></div></div>
      {loading ? <Loading label="Loading your consultation" /> : <>
        {error && <div className="notice error" role="alert">{error}</div>}
        {booking && <><section className="agenda-summary" aria-label="Consultation summary"><span className="module-code">{booking.moduleCode}</span><h2>{booking.moduleName}</h2><p>{booking.tutorName}</p><p>{dateLabel(sgtDate(booking.startTime), { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })} · {timeRange(booking.startTime, booking.endTime)} · SGT</p></section>
          <div className="agenda-layout"><section className="agenda-panel" aria-labelledby="questions-heading"><h2 id="questions-heading">What would you like to discuss?</h2><p className="muted">Include a topic, where you are stuck, and the questions you want to ask.</p><label htmlFor="agenda-questions">Your topics and questions</label><textarea className="form-control" id="agenda-questions" rows={9} value={questions} onChange={event => setQuestions(event.target.value)} aria-describedby="agenda-count agenda-privacy" />
            <p id="agenda-count" className="muted small" aria-live="polite">{[...questions].length.toLocaleString()} / 4,000 characters</p><p id="agenda-privacy" className="muted small">Avoid personal or sensitive information. Only your questions, module code and consultation length are sent to AI. This agenda is not saved or shared with your tutor.</p>
            <button className="btn btn-primary" disabled={pending} onClick={() => draft !== original ? setDialog('regenerate') : void generate()}>{mode ? 'Regenerate agenda' : 'Generate agenda'}</button>{pending && <p role="status">Generating your agenda… You can keep preparing your questions.</p>}
          </section><section className="agenda-panel" aria-labelledby="draft-heading"><h2 id="draft-heading">Your consultation agenda</h2>{mode ? <><p className="muted">AI-generated suggestion. Review and edit before use. {mode === 'fake' && <span className="booking-status">Fake AI output</span>}</p><label htmlFor="agenda-draft">Agenda draft</label><textarea ref={draftRef} id="agenda-draft" className="form-control agenda-draft" rows={14} readOnly={pending} value={draft} onChange={event => { setDraft(event.target.value); setCopyStatus(''); }} /><button className="btn btn-outline-secondary mt-3" onClick={() => void copy()}>Copy agenda</button><p role="status">{copyStatus}</p></> : <p className="muted">Your draft will appear here. AI organizes your questions; it does not answer them.</p>}</section></div></>}
      </>}
    </main>{dialog && <AgendaConfirmation leaving={dialog !== 'regenerate'} close={close} confirm={() => dialog === 'regenerate' ? void generate() : leaveAction.current?.()} />}</div>;
}
