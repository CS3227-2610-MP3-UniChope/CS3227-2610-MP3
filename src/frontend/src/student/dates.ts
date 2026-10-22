const zone = 'Asia/Singapore';
export function sgtDate(value = new Date().toISOString()) { return new Intl.DateTimeFormat('en-CA', { timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(value)); }
export function addDays(date: string, days: number) { const d = new Date(`${date}T00:00:00Z`); d.setUTCDate(d.getUTCDate() + days); return d.toISOString().slice(0, 10); }
export function monday(date: string) { const day = new Date(`${date}T00:00:00Z`).getUTCDay(); return addDays(date, -(day === 0 ? 6 : day - 1)); }
export function validDate(date: string) { return /^\d{4}-\d{2}-\d{2}$/.test(date) && Number.isFinite(Date.parse(`${date}T00:00:00Z`)) && new Date(`${date}T00:00:00Z`).toISOString().slice(0, 10) === date; }
export function dateLabel(date: string, options: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'short' }) { return new Intl.DateTimeFormat('en-SG', { ...options, timeZone: zone }).format(new Date(`${date}T00:00:00+08:00`)); }
export function timeLabel(instant: string) { return new Intl.DateTimeFormat('en-SG', { timeZone: zone, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(instant)); }
export function timeRange(start: string, end: string) { return `${timeLabel(start)} – ${sgtDate(start) !== sgtDate(end) ? `${dateLabel(sgtDate(end))}, ` : ''}${timeLabel(end)} SGT`; }
