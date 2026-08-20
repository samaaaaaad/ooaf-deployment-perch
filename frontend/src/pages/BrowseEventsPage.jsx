import { useMemo, useState } from 'react';

import { useAuth } from '../auth/AuthContext';
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

const categories = [
  { key: 'music', label: 'Music' },
  { key: 'comedy', label: 'Comedy' },
  { key: 'theatre', label: 'Theatre' },
  { key: 'cinema', label: 'Cinema' },
  { key: 'sports', label: 'Sports' },
  { key: 'food', label: 'Food & Drink' },
  { key: 'workshop', label: 'Workshop' },
  { key: 'community', label: 'Community' },
];

// `img` is a concrete keyword handed to the LoremFlickr placeholder service
// so each cover roughly matches the event's theme.
const events = [
  { id: 1, title: 'Sunset Rooftop Sessions', cat: 'music', img: 'concert', venue: 'Rooftop Bar', city: 'Alexandria', date: 'Fri 5 Sep', time: '7:00pm', price: 35 },
  { id: 2, title: 'Thursday Night Laughs', cat: 'comedy', img: 'comedian', venue: 'The Comedy Loft', city: 'Newtown', date: 'Thu 4 Sep', time: '8:00pm', price: 22 },
  { id: 3, title: "A Midsummer Night's Dream", cat: 'theatre', img: 'theatre', venue: 'Old Fitz Theatre', city: 'Woolloomooloo', date: 'Sat 13 Sep', time: '7:30pm', price: 40 },
  { id: 4, title: 'Indie Film Night', cat: 'cinema', img: 'cinema', venue: 'The Vault Cinema', city: 'Surry Hills', date: 'Wed 10 Sep', time: '6:30pm', price: 18 },
  { id: 5, title: 'Inner West Futsal Cup', cat: 'sports', img: 'futsal', venue: 'Marrickville PCYC', city: 'Marrickville', date: 'Sun 21 Sep', time: '10:00am', price: 12 },
  { id: 6, title: 'Natural Wine Tasting', cat: 'food', img: 'winery', venue: 'The Cellar Door', city: 'Chippendale', date: 'Fri 19 Sep', time: '6:00pm', price: 55 },
  { id: 7, title: 'Intro to Pottery', cat: 'workshop', img: 'ceramics', venue: 'Clay & Co Studio', city: 'Erskineville', date: 'Sat 6 Sep', time: '1:00pm', price: 75 },
  { id: 8, title: 'Neighbourhood Trivia Night', cat: 'community', img: 'pub', venue: 'The Local Hall', city: 'Glebe', date: 'Wed 17 Sep', time: '7:00pm', price: 10 },
  { id: 9, title: 'Jazz & Small Plates', cat: 'music', img: 'saxophone', venue: 'Basement Bar', city: 'Sydney CBD', date: 'Sat 27 Sep', time: '8:00pm', price: 30 },
  { id: 10, title: 'Improv Jam Night', cat: 'comedy', img: 'comedy', venue: 'Giant Dwarf', city: 'Redfern', date: 'Fri 26 Sep', time: '8:30pm', price: 20 },
  { id: 11, title: 'One-Act Play Festival', cat: 'theatre', img: 'stage', venue: 'Kings Cross Theatre', city: 'Kings Cross', date: 'Thu 2 Oct', time: '7:00pm', price: 28 },
  { id: 12, title: 'Outdoor Movie: Amélie', cat: 'cinema', img: 'projector', venue: 'Riverside Lawn', city: 'Parramatta', date: 'Sat 4 Oct', time: '7:45pm', price: 15 },
  { id: 13, title: 'Sunrise Yoga & 5K Run', cat: 'sports', img: 'yoga', venue: 'Bondi Pavilion', city: 'Bondi Beach', date: 'Sun 5 Oct', time: '6:30am', price: 18 },
  { id: 14, title: 'Dumpling Masterclass', cat: 'food', img: 'dumpling', venue: 'The Kitchen Studio', city: 'Ashfield', date: 'Sun 12 Oct', time: '11:00am', price: 65 },
  { id: 15, title: 'Film Photography Basics', cat: 'workshop', img: 'camera', venue: 'The Darkroom', city: 'Marrickville', date: 'Sat 11 Oct', time: '2:00pm', price: 80 },
  { id: 16, title: 'Local Makers Market', cat: 'community', img: 'market', venue: 'Leichhardt Town Hall', city: 'Leichhardt', date: 'Sat 18 Oct', time: '9:00am', price: 0 },
];

const labelFor = Object.fromEntries(categories.map((c) => [c.key, c.label]));

export default function BrowseEventsPage() {
  const { user } = useAuth();
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
            <article className={`be-card be-cat-${e.cat}`} key={e.id}>
              <div className="be-cover">
                <img className="be-cover-img" src={`https://loremflickr.com/600/360/${e.img}?lock=${e.id}`} alt="" loading="lazy" />
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
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
