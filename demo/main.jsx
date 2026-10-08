import { render } from 'preact';
import { useMemo, useState } from 'preact/hooks';
import { CalendarFilter, LABELS_EN, LABELS_ES, dayKey } from '../web/CalendarFilter.jsx';
import '../web/calendar-filter.css';

// Three years of made-up entries: most days in some months, none in others.
function sample() {
  const items = [];
  const now = new Date();
  let seed = 7;
  const rand = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
  for (let d = new Date(now.getFullYear() - 2, 0, 1); d <= now; d.setDate(d.getDate() + 1)) {
    const busy = (d.getMonth() + d.getFullYear()) % 5 !== 0;
    if (busy && rand() < 0.45) items.push({ date: new Date(d), title: `Entry for ${d.toDateString()}` });
  }
  return items;
}

function Demo() {
  const items = useMemo(sample, []);
  const counts = useMemo(() => {
    const m = new Map();
    for (const it of items) m.set(dayKey(it.date), (m.get(dayKey(it.date)) ?? 0) + 1);
    return m;
  }, [items]);
  const [when, setWhen] = useState('');
  const [es, setEs] = useState(new URLSearchParams(location.search).get('lang') === 'es');
  const shown = items.filter((it) => !when || dayKey(it.date).startsWith(when));
  return (
    <>
      <h1>CalendarFilter</h1>
      <p>{items.length} sample entries over three years. <a href="#" onClick={(e) => { e.preventDefault(); setEs(!es); }}>{es ? 'English' : 'Español'}</a></p>
      <div className="row">
        <CalendarFilter counts={counts} value={when} onChange={setWhen} locale={es ? 'es' : 'en'} labels={es ? LABELS_ES : LABELS_EN} />
        <span>{shown.length} shown</span>
      </div>
      <ul>{shown.slice(-8).reverse().map((it) => <li key={it.title}>{it.title}</li>)}</ul>
    </>
  );
}

render(<Demo />, document.getElementById('app'));
