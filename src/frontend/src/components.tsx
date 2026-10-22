import type { MouseEvent, ReactNode } from 'react';
import { navigate } from './navigation';
export function Icon({ name }: { name: 'calendar' | 'book' | 'arrow' | 'logout' | 'clock' | 'check' }) {
  const paths = { calendar: 'M7 3v4m10-4v4M4 10h16M5 5h14a1 1 0 0 1 1 1v14H4V6a1 1 0 0 1 1-1Z', book: 'M4 4h7l1 2 1-2h7v15h-7l-1 1-1-1H4V4Zm8 2v14', arrow: 'm9 5 7 7-7 7', logout: 'M10 4H4v16h6m4-12 4 4-4 4m-6-4h10', clock: 'M12 8v5l3 2M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z', check: 'm5 12 4 4L19 6' };
  return <svg className="line-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>;
}
export function Brand() { return <span className="brand"><span className="brand-mark" aria-hidden="true"><Icon name="calendar" /></span><span>UniChope</span></span>; }
export function Link({ href, children, className, current }: { href: string; children: ReactNode; className?: string; current?: boolean }) {
  function click(event: MouseEvent<HTMLAnchorElement>) { if (!event.ctrlKey && !event.metaKey && !event.shiftKey && event.button === 0) { event.preventDefault(); navigate(href); } }
  return <a href={href} onClick={click} className={className} aria-current={current ? 'page' : undefined}>{children}</a>;
}
export function Loading({ label = 'Loading consultations' }: { label?: string }) { return <div className="loading-state" role="status" aria-busy="true"><span className="skeleton" /><span className="skeleton short" /><p>{label}…</p></div>; }
