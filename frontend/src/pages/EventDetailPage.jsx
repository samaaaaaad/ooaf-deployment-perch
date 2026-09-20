import { useEffect, useMemo, useState } from 'react';

import { getEvent as getMockEvent, labelFor } from '../data/events';
import { getEventSeats, getPublishedEvent } from '../api/eventApi';
import { useRouter } from '../utils/router';
import { generateLayout } from './seatLayout';
import './EventDetailPage.css';

const tierName = { vip: 'VIP', premium: 'Premium', standard: 'Standard' };
const statusName = { HELD: 'Held', BOOKED: 'Already booked' };
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function liveLayout(seats, layoutType) {
  if (!seats.length) {
    return { width: 640, height: 280, decor: [], seats: [] };
  }
  const maxX = Math.max(640, ...seats.map((seat) => seat.x + 18));
  const maxY = Math.max(280, ...seats.map((seat) => seat.y + 26));
  const isStage = (layoutType || 'STAGE').toUpperCase() === 'STAGE';
  return {
    width: maxX,
    height: maxY,
    decor: isStage ? [{ shape: 'rect', x: 200, y: 16, w: 240, h: 34, label: 'Stage', kind: 'stage' }] : [],
    seats: seats.map((seat) => ({
      ...seat,
      price: Number(seat.price),
      tier: String(seat.tier).toLowerCase(),
      status: seat.status,
    })),
  };
}

export default function EventDetailPage({ eventId }) {
  const { navigate } = useRouter();
  const isLiveEvent = uuidPattern.test(eventId);
  const mockEvent = isLiveEvent ? null : getMockEvent(eventId);
  const [liveEvent, setLiveEvent] = useState(null);
  const [liveSeats, setLiveSeats] = useState(null);
  const [loading, setLoading] = useState(isLiveEvent);
  const [loadError, setLoadError] = useState('');
  const [selected, setSelected] = useState([]);
  const [hover, setHover] = useState(null);

  useEffect(() => {
    setSelected([]);
    setHover(null);
    if (!isLiveEvent) {
      setLiveEvent(null);
      setLiveSeats(null);
      setLoading(false);
      setLoadError('');
      return undefined;
    }

    let active = true;
    setLoading(true);
    setLoadError('');
    Promise.all([getPublishedEvent(eventId), getEventSeats(eventId)])
      .then(([event, seats]) => {
        if (!active) return;
        setLiveEvent({
          ...event,
          id: event.eventId,
          cat: 'music',
          venue: 'Main Stage Pop-up',
          city: 'Sydney',
          date: 'Upcoming',
          time: '7:30pm',
        });
        setLiveSeats(seats || []);
      })
      .catch((error) => {
        if (active) setLoadError(error.message || 'Unable to load this event');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [eventId, isLiveEvent]);

  useEffect(() => {
    if (!isLiveEvent) return undefined;
    const timer = setInterval(() => {
      getEventSeats(eventId)
        .then((seats) => {
          setLiveSeats(seats || []);
          setSelected((prev) => prev.filter((id) => {
            const seat = (seats || []).find((candidate) => candidate.id === id);
            return seat && seat.status === 'AVAILABLE';
          }));
        })
        .catch(() => {});
    }, 4000);
    return () => clearInterval(timer);
  }, [eventId, isLiveEvent]);

  const event = isLiveEvent ? liveEvent : mockEvent;
  const layout = useMemo(() => {
    if (isLiveEvent) return liveSeats === null ? null : liveLayout(liveSeats, liveEvent?.layoutType);
    return event ? generateLayout(event) : null;
  }, [event, isLiveEvent, liveEvent, liveSeats]);
  const seatById = useMemo(
    () => (layout ? Object.fromEntries(layout.seats.map((s) => [s.id, s])) : {}),
    [layout],
  );

  const tiers = useMemo(() => {
    const map = {};
    layout?.seats.forEach((seat) => { map[seat.tier] = seat.price; });
    return map;
  }, [layout]);

  if (loading) {
    return <div className="ed"><p className="ed-missing">Loading event seats…</p></div>;
  }

  if (!event) {
    return (
      <div className="ed">
        <a className="ed-back" href="/browse" onClick={(e) => { e.preventDefault(); navigate('/browse'); }}>← Back to events</a>
        <p className="ed-missing">{loadError || 'Sorry, that event could not be found.'}</p>
      </div>
    );
  }

  const selectedSeats = selected.map((id) => seatById[id]).filter(Boolean);
  const total = selectedSeats.reduce((sum, s) => sum + s.price, 0);

  function toggle(seat) {
    if (seat.booked || seat.status === 'BOOKED' || seat.status === 'HELD') return;
    setSelected((prev) => (prev.includes(seat.id) ? prev.filter((id) => id !== seat.id) : [...prev, seat.id]));
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
  const unavailable = (seat) => {
    if (seat.status) return seat.status !== 'AVAILABLE';
    return Boolean(seat.booked);
  };
  const statusText = (seat) => seat.status && seat.status !== 'AVAILABLE'
    ? ` · ${statusName[seat.status] || seat.status}`
    : seat.booked ? ' · Already booked' : '';

  return (
    <div className="ed">
      <a className="ed-back" href="/browse" onClick={(e) => { e.preventDefault(); navigate('/browse'); }}>← Back to events</a>

      <header className="ed-head">
        <span className="ed-cat">{labelFor[event.cat]}</span>
        <h1>{event.title}</h1>
        <p className="ed-meta">{event.venue}, {event.city} · {event.date} · {event.time}</p>
      </header>

      {loadError && <p className="ed-missing" role="alert">{loadError}</p>}

      <div className="ed-main">
        <div className="ed-plan">
          <div className="ed-plan-info" role="status">
            {hoverSeat
              ? `Seat ${hoverSeat.label} · ${tierName[hoverSeat.tier]} · $${hoverSeat.price}${statusText(hoverSeat)}`
              : 'Hover a seat to see its price, then click to select'}
          </div>
          <div className="ed-canvas">
            {layout.seats.length === 0 ? (
              <p className="ed-missing">This event does not have a seat map yet.</p>
            ) : (
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
                const isUnavailable = unavailable(s);
                let cls = `ed-seat tier-${s.tier}`;
                if (s.status === 'BOOKED' || s.booked) cls = 'ed-seat is-booked';
                else if (s.status === 'HELD') cls = 'ed-seat is-held';
                else if (isSel) cls = 'ed-seat is-selected';
                return (
                  <circle
                    key={s.id} cx={s.x} cy={s.y} r="9" className={cls}
                    onClick={() => toggle(s)}
                    onMouseEnter={() => setHover(s.id)}
                    onMouseLeave={() => setHover(null)}
                    tabIndex={isUnavailable ? undefined : 0}
                    role="button"
                    aria-label={`Seat ${s.label}, ${tierName[s.tier]}, $${s.price}${statusText(s)}${isSel ? ', selected' : ''}`}
                    onKeyDown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && !isUnavailable) { e.preventDefault(); toggle(s); } }}
                  >
                    <title>{`${s.label} · ${tierName[s.tier]} · $${s.price}${statusText(s)}`}</title>
                  </circle>
                );
              })}
            </svg>
            )}
          </div>
          <div className="ed-legend">
            {tiers.vip !== undefined && <span><i className="sw tier-vip" />VIP ${tiers.vip}</span>}
            {tiers.premium !== undefined && <span><i className="sw tier-premium" />Premium ${tiers.premium}</span>}
            {tiers.standard !== undefined && <span><i className="sw tier-standard" />Standard ${tiers.standard}</span>}
            <span><i className="sw is-selected" />Selected</span>
            <span><i className="sw is-held" />Held</span>
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
