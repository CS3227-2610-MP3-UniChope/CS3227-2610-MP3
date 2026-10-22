import type { Booking, Slot } from '../api';
import { Icon } from '../components';
import { addDays, dateLabel, sgtDate, timeLabel, timeRange } from './dates';
export function Consultation({ item, choose, booking = false }: { item: Slot | Booking; choose: (item: Slot | Booking) => void; booking?: boolean }) {
  const owned = item as Booking;
  return <article className={booking ? 'booking-row' : 'consultation'}><div className="consultation-content"><span className="module-code">{item.moduleCode}</span><h3>{item.moduleName}</h3><p className="tutor-name">{item.tutorName}</p><p className="consultation-time"><Icon name="clock" /><span>{booking && <>{dateLabel(sgtDate(item.startTime), { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })}<br /></>}{timeRange(item.startTime, item.endTime)}</span></p></div><div className="consultation-action">{booking && <span className={`booking-status ${owned.status.toLowerCase()}`}>{owned.status === 'ACTIVE' ? 'Active' : owned.status === 'COMPLETED' ? 'Completed' : 'Cancelled'}</span>}{!booking ? <button className="btn btn-book" onClick={() => choose(item)}>Book</button> : owned.cancellable ? <button className="btn btn-outline-secondary" onClick={() => choose(item)}>Cancel booking</button> : <span className="muted small">{owned.status === 'ACTIVE' ? 'Cancellation unavailable' : ''}</span>}</div></article>;
}
export function Timetable({ slots, week, choose, mobile }: { slots: Slot[]; week: string; choose: (item: Slot) => void; mobile: boolean }) {
  const days = Array.from({ length: 7 }, (_, i) => addDays(week, i));
  // Time bands grow with their contents; overlapping choices never cover each other.
  const times = [...new Set(slots.map(slot => timeLabel(slot.startTime).slice(0, 2)))].sort();
  const hours = times.length ? Array.from({ length: Number(times.at(-1)) - Number(times[0]) + 1 }, (_, i) => String(Number(times[0]) + i).padStart(2, '0')) : [];
  return <>{!mobile && <div className="desktop-timetable" role="region" aria-label="Weekly consultation timetable"><div className="timetable-head"><div className="zone-label">SGT</div>{days.map(day => <div className={`day-header ${day === sgtDate() ? 'today' : ''}`} key={day}><span>{dateLabel(day, { weekday: 'long' })}</span><strong>{dateLabel(day)}</strong></div>)}</div>{hours.map(hour => <div className="time-band" key={hour}><div className="time-label">{hour}:00</div>{days.map(day => <div className="day-cell" key={day}>{slots.filter(slot => sgtDate(slot.startTime) === day && timeLabel(slot.startTime).startsWith(hour)).map(slot => <Consultation key={slot.id} item={slot} choose={item => choose(item)} />)}</div>)}</div>)}</div>}
    {mobile && <div className="mobile-consultations">{days.filter(day => slots.some(slot => sgtDate(slot.startTime) === day)).map(day => <section key={day} className="day-list"><h2>{dateLabel(day, { weekday: 'long', day: 'numeric', month: 'long' })}</h2>{slots.filter(slot => sgtDate(slot.startTime) === day).map(slot => <Consultation key={slot.id} item={slot} choose={item => choose(item)} />)}</section>)}</div>}</>;
}

