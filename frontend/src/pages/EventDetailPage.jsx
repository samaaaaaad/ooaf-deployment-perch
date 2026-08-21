import { useMemo, useState } from 'react';

import { getEvent, labelFor } from '../data/events';
import { useRouter } from '../utils/router';
import { generateLayout } from './seatLayout';
import './EventDetailPage.css';

const tierName = { vip: 'VIP', premium: 'Premium', standard: 'Standard' };

export default function EventDetailPage({ eventId }) {
  const { navigate } = useRouter();
  const event = getEvent(eventId);
  const [selected, setSelected] = useState([]);
  const [hover, setHover] = useState(null);

  const layout = useMemo(() => (event ? generateLayout(event) : null), [event]);
  const seatById = useMemo(
    () => (layout ? Object.fromEntries(layout.seats.map((s) => [s.id, s])) : {}),
    [layout],
  );

  if (!event) {
    return (
      <div className="ed">
        <a className="ed-back" href="/browse" onClick={(e) => { e.preventDefault(); navigate('/browse'); }}>← Back to events</a>
        <p className="ed-missing">Sorry, that event could not be found.</p>
      </div>
    );
  }

  const selectedSeats = selected.map((id) => seatById[id]).filter(Boolean);
  const total = selectedSeats.reduce((sum, s) => sum + s.price, 0);
  const tiers = useMemo(() => {
    const map = {};
    layout.seats.forEach((s) => { map[s.tier] = s.price; });
    return map;
  }, [layout]);

  function toggle(seat) {
    if (seat.booked) return;
    setSelected((prev) => (prev.includes(seat.id) ? prev.filter((x) => x !== seat.id) : [...prev, seat.id]));
  }

  function checkout() {
    const order = {
      title: event.title, venue: event.venue, city: event.city, date: event.date, time: event.time,
      seats: selectedSeats.map((s) => ({ label: s.label, tier: s.tier, price: s.price })), total,
    };
    sessionStorage.setItem('pendingOrder', JSON.stringify(order));
    navigate('/checkout');
  }

  const hoverSeat = hover ? seatById[hover] : null;

  return (
    <div className="ed">
      <a className="ed-back" href="/browse" onClick={(e) => { e.preventDefault(); navigate('/browse'); }}>← Back to events</a>

      <header className="ed-head">
        <span className="ed-cat">{labelFor[event.cat]}</span>
        <h1>{event.title}</h1>
        <p className="ed-meta">{event.venue}, {event.city} · {event.date} · {event.time}</p>
      </header>

      <div className="ed-main">
        <div className="ed-plan">
          <div className="ed-plan-info" role="status">
            {hoverSeat
              ? `Seat ${hoverSeat.label} · ${tierName[hoverSeat.tier]} · $${hoverSeat.price}${hoverSeat.booked ? ' · Already booked' : ''}`
              : 'Hover a seat to see its price, then click to select'}
          </div>
          <div className="ed-canvas">
            <svg viewBox={`0 0 ${layout.width} ${layout.height}`} role="img" aria-label={`Seating plan for ${event.title}`}>
              {layout.decor.map((d, i) => (d.shape === 'rect' ? (
                <g key={i}>
                  <rect x={d.x} y={d.y} width={d.w} height={d.h} rx="9" className={`ed-decor ed-${d.kind}`} />
                  <text x={d.x + d.w / 2} y={d.y + d.h / 2 + 4} textAnchor="middle" className="ed-decor-label">{d.label}</text>
                </g>
              ) : (
                <g key={i}>
                  <ellipse cx={d.cx} cy={d.cy} rx={d.rx} ry={d.ry} className="ed-decor ed-table" />
                  <text x={d.cx} y={d.cy + 4} textAnchor="middle" className="ed-decor-label">{d.label}</text>
                </g>
              )))}

              {layout.seats.map((s) => {
                const isSel = selected.includes(s.id);
                const cls = s.booked ? 'ed-seat is-booked' : isSel ? 'ed-seat is-selected' : `ed-seat tier-${s.tier}`;
                return (
                  <circle
                    key={s.id} cx={s.x} cy={s.y} r="9" className={cls}
                    onClick={() => toggle(s)}
                    onMouseEnter={() => setHover(s.id)}
                    onMouseLeave={() => setHover(null)}
                    tabIndex={s.booked ? undefined : 0}
                    role="button"
                    aria-label={`Seat ${s.label}, ${tierName[s.tier]}, $${s.price}${s.booked ? ', already booked' : isSel ? ', selected' : ''}`}
                    onKeyDown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && !s.booked) { e.preventDefault(); toggle(s); } }}
                  >
                    <title>{`${s.label} · ${tierName[s.tier]} · $${s.price}${s.booked ? ' · Already booked' : ''}`}</title>
                  </circle>
                );
              })}
            </svg>
          </div>
          <div className="ed-legend">
            {tiers.vip !== undefined && <span><i className="sw tier-vip" />VIP ${tiers.vip}</span>}
            {tiers.premium !== undefined && <span><i className="sw tier-premium" />Premium ${tiers.premium}</span>}
            {tiers.standard !== undefined && <span><i className="sw tier-standard" />Standard ${tiers.standard}</span>}
            <span><i className="sw is-selected" />Selected</span>
            <span><i className="sw is-booked" />Booked</span>
          </div>
        </div>

        <aside className="ed-cart">
          <h2>Your selection</h2>
          {selectedSeats.length === 0 ? (
            <p className="ed-empty">No seats selected yet. Pick seats from the plan on the left.</p>
          ) : (
            <ul className="ed-list">
              {selectedSeats.map((s) => (
                <li key={s.id}>
                  <span className="ed-li-seat"><strong>Seat {s.label}</strong><small>{tierName[s.tier]}</small></span>
                  <span className="ed-li-price">${s.price}</span>
                  <button type="button" className="ed-remove" aria-label={`Remove seat ${s.label}`} onClick={() => toggle(s)}>×</button>
                </li>
              ))}
            </ul>
          )}
          <div className="ed-total"><span>Total</span><strong>${total}</strong></div>
          <button type="button" className="ed-next" disabled={selectedSeats.length === 0} onClick={checkout}>
            Continue to payment →
          </button>
          <p className="ed-cart-note">{selectedSeats.length} {selectedSeats.length === 1 ? 'seat' : 'seats'} selected</p>
        </aside>
      </div>
    </div>
  );
}
