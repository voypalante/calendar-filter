// CalendarFilter: a date filter that stays the same size however many years of data you have.
// It opens on a year of months with counts, then a month of days with a dot where something
// exists. Pick a whole year, a month or a day. Preact; for React, import the hooks from 'react'.
//
//   <CalendarFilter counts={countsByDay} value={when} onChange={setWhen} />
//
// counts: Map of "YYYY-MM-DD" -> number of items that day (use dayKey to build the keys).
// value:  '' (any time) | 'YYYY' | 'YYYY-MM' | 'YYYY-MM-DD'. Filter with dayKey(date).startsWith(value).
import { useEffect, useMemo, useRef, useState } from 'preact/hooks';

const pad = (n) => String(n).padStart(2, '0');

/** "YYYY-MM-DD" in local time. */
export const dayKey = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

export const LABELS_EN = {
  anyTime: 'Any time',
  when: 'When',
  previousYear: 'Previous year',
  nextYear: 'Next year',
  previousMonth: 'Previous month',
  nextMonth: 'Next month',
  allMonthsOf: (year) => `All months of ${year}`,
  count: (n) => `${n} ${n === 1 ? 'entry' : 'entries'}`,
};

export const LABELS_ES = {
  anyTime: 'Cualquier fecha',
  when: 'Cuándo',
  previousYear: 'Año anterior',
  nextYear: 'Año siguiente',
  previousMonth: 'Mes anterior',
  nextMonth: 'Mes siguiente',
  allMonthsOf: (year) => `Todos los meses de ${year}`,
  count: (n) => `${n} ${n === 1 ? 'entrada' : 'entradas'}`,
};

/** First day of the week for a locale: 0 Sunday, 1 Monday. Uses Intl when the browser knows it. */
export function weekStart(locale) {
  try {
    const info = new Intl.Locale(locale).getWeekInfo?.() ?? new Intl.Locale(locale).weekInfo;
    if (info?.firstDay) return info.firstDay % 7;
  } catch {}
  return /^en-(US|CA|PH)|^(he|ja|ko|zh-TW|pt-BR)|-(US|PA|MX)$/i.test(locale) ? 0 : 1;
}

/** Text for the button: "Any time", "2026", "October 2026", "Oct 6, 2026". */
export function filterLabel(value, locale = 'en', labels = LABELS_EN) {
  if (!value) return labels.anyTime;
  const [y, m, d] = value.split('-').map(Number);
  if (!m) return String(y);
  if (!d) return new Intl.DateTimeFormat(locale, { month: 'long', year: 'numeric' }).format(new Date(y, m - 1, 1));
  return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(y, m - 1, d));
}

const Chevron = ({ flip }) => (
  <svg className="cf-icon" viewBox="0 0 24 24" aria-hidden="true" style={flip ? { rotate: '180deg' } : undefined}>
    <path d="M15 18l-6-6 6-6" />
  </svg>
);

const CalendarIcon = () => (
  <svg className="cf-icon" viewBox="0 0 24 24" aria-hidden="true">
    <rect x="3.5" y="5" width="17" height="15.5" rx="2.5" />
    <path d="M3.5 10h17M8 3v4M16 3v4" />
  </svg>
);

export function CalendarFilter({ counts, value, onChange, locale = 'en', labels = LABELS_EN, firstDay }) {
  const [open, setOpen] = useState(false);
  const wrap = useRef();
  const now = new Date();
  const years = useMemo(() => [...new Set([...counts.keys()].map((k) => Number(k.slice(0, 4))))].sort((a, b) => a - b), [counts]);
  const initialYear = () => (value ? Number(value.slice(0, 4)) : years.at(-1) ?? now.getFullYear());
  const initialMonth = () => (value?.length >= 7 ? Number(value.slice(5, 7)) : null);
  const [year, setYear] = useState(initialYear);
  const [month, setMonth] = useState(initialMonth); // 1-12, or null for the year view

  // Reopen where the current value is; close on outside click or Escape.
  useEffect(() => {
    if (!open) return;
    setYear(initialYear());
    setMonth(initialMonth());
    const close = (e) => { if (!wrap.current?.contains(e.target)) setOpen(false); };
    const esc = (e) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('pointerdown', close);
    document.addEventListener('keydown', esc);
    return () => { document.removeEventListener('pointerdown', close); document.removeEventListener('keydown', esc); };
  }, [open]);

  const sum = (prefix) => { let n = 0; for (const [k, c] of counts) if (k.startsWith(prefix)) n += c; return n; };
  const pick = (v) => { onChange(v); setOpen(false); };
  const minYear = years[0] ?? now.getFullYear();
  const maxYear = Math.max(years.at(-1) ?? now.getFullYear(), now.getFullYear());
  const monthName = (m, style = 'short') => new Intl.DateTimeFormat(locale, { month: style }).format(new Date(2026, m - 1, 1));
  const first = firstDay ?? weekStart(locale);
  const weekdays = useMemo(() => Array.from({ length: 7 }, (_, i) => {
    const index = (first + i) % 7;
    // 2026-10-04 is a Sunday.
    return { index, label: new Intl.DateTimeFormat(locale, { weekday: 'narrow' }).format(new Date(2026, 9, 4 + index)) };
  }), [locale, first]);

  let body;
  if (month == null) {
    const key = String(year);
    body = (
      <>
        <div className="cf-head">
          <button className="cf-nav" aria-label={labels.previousYear} disabled={year <= minYear} onClick={() => setYear(year - 1)}><Chevron /></button>
          <button className={`cf-title ${value === key ? 'on' : ''}`} onClick={() => pick(key)} disabled={!sum(key)}>
            {year}<small>{labels.count(sum(key))}</small>
          </button>
          <button className="cf-nav" aria-label={labels.nextYear} disabled={year >= maxYear} onClick={() => setYear(year + 1)}><Chevron flip /></button>
        </div>
        <div className="cf-months">
          {Array.from({ length: 12 }, (_, i) => {
            const mk = `${year}-${pad(i + 1)}`;
            const n = sum(mk);
            return (
              <button key={mk} className={`cf-month ${value?.startsWith(mk) ? 'on' : ''}`} disabled={!n} onClick={() => setMonth(i + 1)}>
                <span>{monthName(i + 1)}</span>
                {n > 0 && <small>{n}</small>}
              </button>
            );
          })}
        </div>
      </>
    );
  } else {
    const key = `${year}-${pad(month)}`;
    const lead = (new Date(year, month - 1, 1).getDay() - weekdays[0].index + 7) % 7;
    const days = new Date(year, month, 0).getDate();
    const step = (dir) => {
      let m = month + dir, y = year;
      if (m < 1) { m = 12; y--; } else if (m > 12) { m = 1; y++; }
      setYear(y); setMonth(m);
    };
    body = (
      <>
        <div className="cf-head">
          <button className="cf-nav" aria-label={labels.previousMonth} disabled={year <= minYear && month === 1} onClick={() => step(-1)}><Chevron /></button>
          <button className={`cf-title ${value === key ? 'on' : ''}`} onClick={() => pick(key)} disabled={!sum(key)}>
            {monthName(month, 'long')} {year}<small>{labels.count(sum(key))}</small>
          </button>
          <button className="cf-nav" aria-label={labels.nextMonth} disabled={year >= maxYear && month === 12} onClick={() => step(1)}><Chevron flip /></button>
        </div>
        <div className="cf-days">
          {weekdays.map((w) => <span className="cf-wd" key={w.index}>{w.label}</span>)}
          {Array.from({ length: lead }, (_, i) => <span key={`lead${i}`} />)}
          {Array.from({ length: days }, (_, i) => {
            const k = `${key}-${pad(i + 1)}`;
            const n = counts.get(k) ?? 0;
            return (
              <button key={k} className={`cf-day ${value === k ? 'on' : ''} ${k === dayKey(now) ? 'today' : ''}`} disabled={!n}
                onClick={() => pick(k)} title={n ? labels.count(n) : undefined}>
                {i + 1}{n > 0 && <i />}
              </button>
            );
          })}
        </div>
        <button className="cf-link cf-up" onClick={() => setMonth(null)}>{labels.allMonthsOf(year)}</button>
      </>
    );
  }

  return (
    <div className="cf" ref={wrap}>
      <button className={`cf-button ${value ? 'set' : ''}`} aria-haspopup="dialog" aria-expanded={open} onClick={() => setOpen(!open)}>
        <CalendarIcon />
        <span>{filterLabel(value, locale, labels)}</span>
      </button>
      {open && (
        <div className="cf-pop" role="dialog" aria-label={labels.when}>
          {body}
          <div className="cf-foot">
            <button className="cf-link" onClick={() => pick('')}>{labels.anyTime}</button>
          </div>
        </div>
      )}
    </div>
  );
}
