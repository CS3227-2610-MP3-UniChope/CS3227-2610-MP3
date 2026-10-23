import { useEffect, useRef } from 'react';

export default function AgendaConfirmation({ leaving, close, confirm }: { leaving: boolean; close: () => void; confirm: () => void }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const trigger = document.activeElement as HTMLElement | null;
    ref.current?.querySelector('button')?.focus();
    const key = (event: KeyboardEvent) => {
      if (event.key === 'Escape') { event.preventDefault(); close(); }
      if (event.key === 'Tab') {
        const controls = Array.from(ref.current?.querySelectorAll('button') || []);
        const first = controls[0], last = controls.at(-1);
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
      }
    };
    document.addEventListener('keydown', key);
    return () => { document.removeEventListener('keydown', key); if (trigger?.isConnected) trigger.focus(); };
  }, [close]);
  return <div className="dialog-backdrop"><div ref={ref} className="confirmation" role="dialog" aria-modal="true" aria-labelledby="agenda-confirm-title" aria-describedby="agenda-confirm-description">
    <h2 id="agenda-confirm-title">{leaving ? 'Leave preparation?' : 'Replace your edited draft?'}</h2>
    <p id="agenda-confirm-description">{leaving ? 'Your questions and draft are only kept on this page. Leaving will discard them.' : 'Generating again will replace your edits after a new agenda is ready.'}</p>
    <div className="dialog-actions"><button className="btn btn-outline-secondary" onClick={close}>{leaving ? 'Keep preparing' : 'Keep draft'}</button><button className="btn btn-primary" onClick={confirm}>{leaving ? 'Leave page' : 'Regenerate agenda'}</button></div>
  </div></div>;
}
