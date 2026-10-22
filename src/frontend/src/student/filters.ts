import { addDays, monday, sgtDate, validDate } from './dates';
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export function filters(search: string, bookings: boolean) {
  const original = new URLSearchParams(search); const clean = new URLSearchParams(); let invalid = false;
  const allowed = bookings ? ['view', 'date', 'status'] : ['moduleId', 'date', 'week'];
  for (const key of original.keys()) if (!allowed.includes(key) || original.getAll(key).length > 1) invalid = true;
  const dateRaw = original.get('date') || ''; const date = validDate(dateRaw) ? dateRaw : ''; if (dateRaw && !date) invalid = true;
  const moduleRaw = original.get('moduleId') || ''; const moduleId = uuid.test(moduleRaw) ? moduleRaw : ''; if (!bookings && moduleRaw && !moduleId) invalid = true;
  let week = original.get('week') || ''; if (!validDate(week)) { if (week) invalid = true; week = monday(date || sgtDate()); }
  else if (week !== monday(week)) { invalid = true; week = monday(week); }
  if (date) week = monday(date);
  const rawView = original.get('view'); const view = rawView === 'history' ? 'history' : 'upcoming'; if (bookings && rawView && !['history', 'upcoming'].includes(rawView)) invalid = true;
  const rawStatus = original.get('status') || ''; const status = view === 'history' && ['CANCELLED', 'COMPLETED'].includes(rawStatus) ? rawStatus : ''; if (bookings && rawStatus && !status) invalid = true;
  if (bookings) { if (rawView || view === 'history') clean.set('view', view); if (status) clean.set('status', status); }
  else { if (moduleId) clean.set('moduleId', moduleId); if (original.has('week') || date) clean.set('week', week); }
  if (date) clean.set('date', date);
  return { date, moduleId, week, end: addDays(week, 6), view, status, invalid, clean };
}
