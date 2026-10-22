import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, expect, test, vi } from 'vitest';
import App from '../App';
import { apiFixture, deferred, failure, ids, json, slot, student } from './api-fixture';
const key = 'unichope.theme';
const toggle = () => screen.getByRole('button', { name: 'Dark theme' });
function theme(value: string) {
  expect(document.documentElement).toHaveAttribute('data-bs-theme', value);
  expect(toggle()).toHaveAttribute('aria-pressed', String(value === 'dark'));
}
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });
async function signIn(user: ReturnType<typeof userEvent.setup>) {
  await user.type(await screen.findByLabelText(/^email$/i), 'synthetic@example.test');
  await user.type(screen.getByLabelText(/^password$/i), 'Synthetic-password-12');
  await user.click(screen.getByRole('button', { name: /^sign in$/i }));
  await screen.findByRole('button', { name: /^sign out$/i });
}
for (const value of [null, 'light', 'dark', 'Dark', 'blue', '', '{"theme":"dark"}']) {
 test('TH-DEFAULT/TH-STORAGE-FALLBACK exact stored value ' + value, async () => {
  if (value !== null) localStorage.setItem(key, value);
  const writes = vi.spyOn(Storage.prototype, 'setItem');
  apiFixture(); window.history.replaceState({}, '', '/login'); render(<App />);
  await screen.findByLabelText(/^email$/i); theme(value === 'dark' ? 'dark' : 'light');
  expect(writes).not.toHaveBeenCalled();
 });
}
for (const state of ['login','restoring','failure','notfound','slots','bookings','TUTOR','ADMIN']) {
 test('TH-PLACEMENT/TH-A11Y one banner switch ' + state, async () => {
  const api = apiFixture(state === 'login' ? null : { ...student, role: state === 'TUTOR' || state === 'ADMIN' ? state : 'STUDENT' });
  const pending = deferred<Response>();
  if (state === 'restoring') api.on('GET', '/api/auth/me', () => pending.promise);
  if (state === 'failure') api.on('GET', '/api/auth/me', () => failure(503,'TEMPORARILY_UNAVAILABLE'));
  window.history.replaceState({}, '', state === 'notfound' ? '/unknown' : state === 'bookings' ? '/student/bookings' : '/student/slots?week=2026-10-19');
  render(<App />);
  if (state === 'login') await screen.findByLabelText(/^email$/i);
  else if (state === 'failure') await screen.findByRole('alert');
  else if (state === 'notfound') await screen.findByRole('heading', { name: /not found/i });
  else if (state !== 'restoring') await screen.findByRole('button', { name: /^sign out$/i });
  expect(screen.getAllByRole('banner')).toHaveLength(1);
  expect(screen.getAllByRole('button', { name: 'Dark theme' })).toHaveLength(1);
  expect(within(screen.getByRole('banner')).getByRole('button', { name: 'Dark theme' })).toBe(toggle());
  expect(toggle()).toHaveAttribute('type','button'); theme('light');
  if (state === 'slots' || state === 'bookings') { expect(screen.getByText(student.name)).toBeVisible(); expect(screen.getByRole('navigation')).toBeVisible(); }
 });
}
test('TH-TOGGLE/TH-A11Y Enter and Space toggle once, keep focus and write literal once per activation', async () => {
 apiFixture(); render(<App />); await screen.findByLabelText(/^email$/i);
 const writes=vi.spyOn(Storage.prototype,'setItem'); const remove=vi.spyOn(Storage.prototype,'removeItem'); const clear=vi.spyOn(Storage.prototype,'clear');
 const user=userEvent.setup(); const url=window.location.href; const email=screen.getByLabelText(/^email$/i);
 toggle().focus(); const initial=toggle().innerHTML;
 await user.keyboard('{Enter}'); theme('dark'); expect(toggle()).toHaveFocus();
 expect(toggle().innerHTML).not.toBe(initial);
 expect(writes.mock.calls).toEqual([[key,'dark']]); expect(writes.mock.instances[0]).toBe(localStorage);
 await user.keyboard(' '); theme('light'); expect(toggle()).toHaveFocus();
 expect(writes.mock.calls).toEqual([[key,'dark'],[key,'light']]); expect(writes.mock.instances[1]).toBe(localStorage);
 expect(remove).not.toHaveBeenCalled(); expect(clear).not.toHaveBeenCalled();
 expect(window.location.href).toBe(url); expect(screen.getByLabelText(/^email$/i)).toBe(email);
 await user.tab(); expect(toggle()).not.toHaveFocus();
});
for (const kind of ['getItem','access','setItem']) test('TH-STORAGE-FALLBACK '+kind+' throws safely', async () => {
 if (kind === 'access') vi.spyOn(window,'localStorage','get').mockImplementation(() => { throw new Error('blocked'); });
 else vi.spyOn(Storage.prototype,kind).mockImplementation(() => { throw new Error('blocked'); });
 apiFixture(); render(<App />); await screen.findByLabelText(/^email$/i); theme('light');
 const user=userEvent.setup(); await user.click(toggle()); theme('dark'); await user.click(toggle()); theme('light');
 expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});
test('TH-SESSION S1 switching preserves loaded actor/filter/notice/URL/results and sends no request', async () => {
 const api=apiFixture(student); window.history.replaceState({}, '', `/student/slots?week=2026-10-19&moduleId=${ids.module}&bad=1`);
 render(<App />); await screen.findByText(slot.tutorName); const notice=screen.getByRole('alert').textContent;
 const url=window.location.href, count=api.calls.length; const user=userEvent.setup(); await user.click(toggle());
 theme('dark'); expect(api.calls).toHaveLength(count); expect(window.location.href).toBe(url);
 expect(screen.getByRole('alert')).toHaveTextContent(notice!); expect(screen.getByText(student.name)).toBeVisible();
 expect(screen.getByRole('combobox', { name: /module/i })).toHaveValue(ids.module); expect(screen.getByText(slot.tutorName)).toBeVisible();
});
for (const page of ['slots','bookings']) test('TH-SESSION S2 pending '+page+' survives toggle', async () => {
 const api=apiFixture(student); const pending=deferred<Response>(); api.on('GET','/api/student/'+page,()=>pending.promise);
 window.history.replaceState({}, '', '/student/'+page+'?week=2026-10-19'); render(<App />);
 await waitFor(()=>expect(api.requests('/api/student/'+page)).toHaveLength(1));
 const count=api.calls.length; await userEvent.setup().click(toggle()); theme('dark'); expect(api.calls).toHaveLength(count);
 expect(api.requests('/api/student/'+page)[0].init.signal?.aborted).not.toBe(true);
 await act(async()=>pending.resolve(json(page === 'slots' ? [slot] : [{ ...slot,id:ids.booking,slotId:slot.id,status:'ACTIVE',cancellable:true }])));
 await screen.findByText(slot.tutorName); expect(api.requests('/api/student/'+page)).toHaveLength(1);
});
for (const source of ['csrf','post']) for (const outcome of ['success','401','inactive','503','network','csrf-invalid','malformed']) {
 test('TH-SESSION/TH-PERSIST dark survives logout '+source+' '+outcome, async () => {
  localStorage.setItem(key,'dark'); const api=apiFixture(student);
  if (outcome !== 'success') api.on(source === 'csrf' ? 'GET' : 'POST',source === 'csrf' ? '/api/auth/csrf' : '/api/auth/logout',()=> {
    if(outcome === 'network') throw new TypeError('synthetic');
    if(outcome === 'malformed') return json({invalid:true});
    return failure(outcome === '401' ? 401 : outcome === '503' ? 503 : 403,outcome === 'inactive' ? 'ACCOUNT_INACTIVE' : outcome === 'csrf-invalid' ? 'CSRF_INVALID' : 'UNAUTHENTICATED');
  });
  const writes=vi.spyOn(Storage.prototype,'setItem'),remove=vi.spyOn(Storage.prototype,'removeItem'),clear=vi.spyOn(Storage.prototype,'clear');
  window.history.replaceState({}, '', '/student/slots?week=2026-10-19'); render(<App />); await screen.findByText(slot.tutorName);
  await userEvent.setup().click(screen.getByRole('button', { name:/^sign out$/i }));
  await screen.findByLabelText(/^email$/i); await waitFor(()=>expect(screen.getByRole('button',{name:/^sign in$/i})).toBeEnabled());
  theme('dark'); expect(localStorage.getItem(key)).toBe('dark'); expect(writes).not.toHaveBeenCalled(); expect(remove).not.toHaveBeenCalled(); expect(clear).not.toHaveBeenCalled();
  expect(screen.queryByText(student.name)).not.toBeInTheDocument(); expect(screen.queryByText(slot.tutorName)).not.toBeInTheDocument();
 });
}
for (const [status,code] of [[401,'UNAUTHENTICATED'],[403,'ACCOUNT_INACTIVE'],[403,'FORBIDDEN']] as const) test('TH-SESSION dark survives protected '+code, async () => {
 localStorage.setItem(key,'dark');const api=apiFixture(student);api.on('GET','/api/student/slots',()=>failure(status,code));
 window.history.replaceState({}, '', '/student/slots');render(<App />);await screen.findByRole('alert');theme('dark');expect(localStorage.getItem(key)).toBe('dark');
});
for (const role of ['STUDENT','TUTOR','ADMIN'] as const) test('TH-PERSIST role '+role+' login/logout/account change and reload with no implicit storage writes', async () => {
 window.history.replaceState({}, '', '/login'); const api=apiFixture(); api.on('POST','/api/auth/login',()=>json({...student,role}));
 const view=render(<App />);await screen.findByLabelText(/^email$/i);const user=userEvent.setup();await user.click(toggle());theme('dark');
 const writes=vi.spyOn(Storage.prototype,'setItem'),remove=vi.spyOn(Storage.prototype,'removeItem'),clear=vi.spyOn(Storage.prototype,'clear');
 await signIn(user);theme('dark');await user.click(screen.getByRole('button',{name:/^sign out$/i}));await screen.findByLabelText(/^email$/i);theme('dark');
 api.on('POST','/api/auth/login',()=>json({...student,id:'10000000-0000-4000-8000-000000000099',name:'Other synthetic actor',role:'STUDENT'}));
 await signIn(user);theme('dark');expect(screen.getByText('Other synthetic actor')).toBeVisible();
 expect(writes).not.toHaveBeenCalled();expect(remove).not.toHaveBeenCalled();expect(clear).not.toHaveBeenCalled();
 view.unmount();apiFixture();render(<App />);await screen.findByLabelText(/^email$/i);theme('dark');
 await user.click(toggle());theme('light');expect(writes.mock.calls).toEqual([[key,'light']]);
});
test('TH-PERSIST no storage writes for booking/cancellation and restoration failure', async () => {
 localStorage.setItem(key,'dark'); const api=apiFixture(student);window.history.replaceState({}, '', '/student/slots?week=2026-10-19');const view=render(<App />);
 const writes=vi.spyOn(Storage.prototype,'setItem'),remove=vi.spyOn(Storage.prototype,'removeItem'),clear=vi.spyOn(Storage.prototype,'clear');const user=userEvent.setup();
 await user.click(await screen.findByRole('button',{name:/^book$/i}));await user.click(within(await screen.findByRole('dialog')).getByRole('button',{name:/confirm/i}));await screen.findByText(/booking confirmed/i);
 await user.click(screen.getByRole('link',{name:/my bookings/i}));await user.click(await screen.findByRole('button',{name:/^cancel(?: booking)?$/i}));await user.click(within(await screen.findByRole('dialog')).getByRole('button',{name:/confirm/i}));await screen.findByText(/booking cancelled/i);
 view.unmount();api.on('GET','/api/auth/me',()=>failure(503,'TEMPORARILY_UNAVAILABLE'));render(<App />);await screen.findByRole('alert');theme('dark');
 expect(writes).not.toHaveBeenCalled();expect(remove).not.toHaveBeenCalled();expect(clear).not.toHaveBeenCalled();
});

test('TH-STORAGE-FALLBACK write failure keeps current theme, reload uses stored rule',async()=>{
 localStorage.setItem(key,'dark');apiFixture();const writes=vi.spyOn(Storage.prototype,'setItem').mockImplementation(()=>{throw new Error('quota')});
 const view=render(<App />);await screen.findByLabelText(/^email$/i);theme('dark');await userEvent.setup().click(toggle());theme('light');
 expect(localStorage.getItem(key)).toBe('dark');expect(screen.queryByRole('alert')).not.toBeInTheDocument();
 view.unmount();render(<App />);await screen.findByLabelText(/^email$/i);theme('dark');expect(writes).toHaveBeenCalledTimes(1);
});
test('TH-SESSION protected CSRF denial preserves dark and no automatic replay or storage operation',async()=>{
 localStorage.setItem(key,'dark');const api=apiFixture(student);api.on('POST','/api/student/bookings',()=>failure(403,'CSRF_INVALID'));
 window.history.replaceState({}, '', '/student/slots?week=2026-10-19');render(<App />);
 const writes=vi.spyOn(Storage.prototype,'setItem'),remove=vi.spyOn(Storage.prototype,'removeItem');const user=userEvent.setup();
 await user.click(await screen.findByRole('button',{name:/^book$/i}));await user.click(within(await screen.findByRole('dialog')).getByRole('button',{name:/confirm/i}));
 await screen.findByRole('alert');theme('dark');expect(api.requests('/api/student/bookings','POST')).toHaveLength(1);
 expect(writes).not.toHaveBeenCalled();expect(remove).not.toHaveBeenCalled();expect(localStorage.getItem(key)).toBe('dark');
});
