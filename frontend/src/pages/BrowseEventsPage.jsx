import { useMemo, useState } from 'react';

import { useAuth } from '../auth/AuthContext';
import { categories, events, labelFor } from '../data/events';
import { useRouter } from '../utils/router';
import './BrowseEventsPage.css';

const pinIcon = (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 21s7-5.5 7-11a7 7 0 1 0-14 0c0 5.5 7 11 7 11Z" /><circle cx="12" cy="10" r="2.5" />
  </svg>
);
const searchIcon = (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="11" cy="11" r="7" /><path d="m20 20-3.2-3.2" />
  </svg>
);

export default function BrowseEventsPage() {
  const { user } = useAuth();
  const { navigate } = useRouter();
  const [active, setActive] = useState('all');
  const [query, setQuery] = useState('');

  const firstName = user.fullName.split(' ')[0];

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return events.filter((e) => {
      if (active !== 'all' && e.cat !== active) return false;
      if (!q) return true;
      return `${e.title} ${e.venue} ${e.city}`.toLowerCase().includes(q);
    });
  }, [active, query]);

  function openEvent(id) {
    return (event) => { event.preventDefault(); navigate(`/event/${id}`); };
  }

  return (
    <section className="be">
      <header className="be-head">
        <p className="be-eyebrow">Discover</p>
        <h1>What&apos;s on, {firstName}?</h1>
        <p className="be-sub">Browse events near you, from intimate gigs and comedy nights to pop-up dinners.</p>
        <div className="be-search">
          <span className="be-search-icon" aria-hidden="true">{searchIcon}</span>
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search events or venues"
            aria-label="Search events or venues"
          />
        </div>
      </header>

      <div className="be-filters" role="tablist" aria-label="Filter by category">
        <button type="button" className={`be-chip${active === 'all' ? ' is-active' : ''}`} onClick={() => setActive('all')}>
          All events
        </button>
        {categories.map((c) => (
          <button key={c.key} type="button" className={`be-chip${active === c.key ? ' is-active' : ''}`} onClick={() => setActive(c.key)}>
            {c.label}
          </button>
        ))}
      </div>

      <p className="be-count">
        {filtered.length} {filtered.length === 1 ? 'event' : 'events'}
        {active !== 'all' && ` in ${labelFor[active]}`}
      </p>

      {filtered.length === 0 ? (
        <div className="be-empty">
          <h3>No events match your search</h3>
          <p>Try another category or clear the search box.</p>
        </div>
      ) : (
        <div className="be-grid">
          {filtered.map((e) => (
            <a className={`be-card be-cat-${e.cat}`} key={e.id} href={`/event/${e.id}`} onClick={openEvent(e.id)}>
              <div className="be-cover">
                <img className="be-cover-img" src={`/events/event-${e.id}.jpg`} alt="" loading="lazy" />
                <span className="be-cover-scrim" aria-hidden="true" />
                <div className="be-cover-top">
                  <span className="be-pill">{labelFor[e.cat]}</span>
                  <span className="be-date">{e.date} · {e.time}</span>
                </div>
              </div>
              <div className="be-body">
                <h3 className="be-title">{e.title}</h3>
                <span className="be-loc"><span className="be-mi" aria-hidden="true">{pinIcon}</span>{e.venue}, {e.city}</span>
                <div className="be-foot">
                  <span className="be-price">{e.price === 0 ? 'Free' : `From $${e.price}`}</span>
                  <span className="be-view">View seats →</span>
                </div>
              </div>
            </a>
          ))}
        </div>
      )}
    </section>
  );
}
