import { useEffect, useRef } from 'react';
import type { Booking, Slot } from '../api';
import { dateLabel, sgtDate, timeRange } from './dates';
export default function Confirmation({ item, cancelling, pending, close, confirm, signOut }: { item: Slot | Booking; cancelling: boolean; pending: boolean; close: () => void; confirm: () => void; signOut: () => void }) {
  const ref = useRef<HTMLDivElement>(null); const pendingRef = useRef(pending); pendingRef.current = pending;
  useEffect(() => {
    const trigger = document.activeElement as HTMLElement | null;
    ref.current?.querySelector<HTMLElement>('button')?.focus();
    function key(event: KeyboardEvent) {
      if (event.key === 'Escape' && !pendingRef.current) { event.preventDefault(); close(); }
      if (event.key === 'Tab') {
        const controls = Array.from(ref.current?.querySelectorAll<HTMLElement>('button:not(:disabled),a[href],input:not(:disabled)') || []);
        if (!controls.length) { event.preventDefault(); ref.current?.focus(); return; }
        const first = controls[0], last = controls.at(-1)!;
        if (event.shiftKey && (document.activeElement === first || !ref.current?.contains(document.activeElement))) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && (document.activeElement === last || !ref.current?.contains(document.activeElement))) { event.preventDefault(); first.focus(); }
      }
    }
    document.addEventListener('keydown', key);
    return () => { document.removeEventListener('keydown', key); if (trigger?.isConnected) trigger.focus(); else document.querySelector<HTMLElement>('h1')?.focus(); };
  }, [close]);
  return <div className="dialog-backdrop"><div ref={ref} className="confirmation" role="dialog" aria-modal="true" aria-labelledby="confirm-title" aria-describedby="confirm-description" tabIndex={-1}>
    <span className="eyebrow">{cancelling ? 'CHANGE OF PLANS' : 'MAKE TIME FOR LEARNING'}</span><h2 id="confirm-title">{cancelling ? 'Cancel this booking?' : 'Book this consultation?'}</h2><p id="confirm-description">{cancelling ? 'Your place will become available to another student.' : 'Check the details before confirming your place.'}</p>
    <div className="confirmation-details"><strong className="module-code">{item.moduleCode}</strong><h3>{item.moduleName}</h3><p>{item.tutorName}</p><p>{dateLabel(sgtDate(item.startTime), { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}<br /><strong>{timeRange(item.startTime, item.endTime)}</strong></p></div>
    {pending && <><p role="status">Submitting… Please wait for confirmation. Your booking or cancellation may still complete after you end this session. Signing out does not cancel an operation already sent.</p><button className="text-button mb-3" onClick={signOut}>End session</button></>}
    <div className="dialog-actions"><button className="btn btn-outline-secondary" onClick={close} disabled={pending}>{cancelling ? 'Keep booking' : 'Go back'}</button><button className="btn btn-primary" onClick={confirm} disabled={pending}>{pending ? 'Confirming…' : cancelling ? 'Confirm cancellation' : 'Confirm booking'}</button></div>
  </div></div>;
}
