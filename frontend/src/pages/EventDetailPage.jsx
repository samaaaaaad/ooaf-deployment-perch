import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { getEvent as getMockEvent, labelFor } from '../data/events';
import { getEventSeats, getMyHolds, getPublishedEvent, holdSeat, releaseSeatHold } from '../api/eventApi';
import { useAuth } from '../auth/AuthContext';
import { decorFromLayoutJson } from '../utils/floorPlanDecor';
import { formatCountdown } from '../utils/formatCountdown';
import { useRouter } from '../utils/router';
import { discounted, isStudent, money } from '../utils/studentPricing';
import { generateLayout } from './seatLayout';
import './EventDetailPage.css';

const tierName = { vip: 'VIP', premium: 'Premium', standard: 'Standard' };
const statusName = { HELD: 'Held', BOOKED: 'Already booked' };
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const POLL_MS = 4000;
const URGENT_MS = 60 * 1000;

function toHold(hold) {
  return { seatId: hold.seatId, expiresAtMs: Date.parse(hold.holdExpiresAt) };
}

function toHoldMap(list) {
  const map = {};
  (list || []).forEach((hold) => { map[hold.seatId] = toHold(hold); });
  return map;
}

function without(map, key) {
  const { [key]: removed, ...rest } = map; // eslint-disable-line no-unused-vars
  return rest;
}

// Custom (builder-drawn) layouts are authored on a 900x560 board (see
// FloorPlanEditorPage/BOARD_W/BOARD_H) and Stage layouts on a 640x280 grid
// (see EventService#stageSeats). Reusing those same fixed canvases here keeps
// a seat's on-screen position identical to where it was placed in the builder,
// instead of cropping the viewBox to the seats' bounding box.
const STAGE_W = 640;
const STAGE_H = 280;
const CUSTOM_W = 900;
const CUSTOM_H = 560;

function liveLayout(seats, layoutType, layoutJson) {
  const isStage = (layoutType || 'STAGE').toUpperCase() === 'STAGE';
  const width = isStage ? STAGE_W : CUSTOM_W;
  const height = isStage ? STAGE_H : CUSTOM_H;
  const decor = isStage
    ? [{ shape: 'rect', x: 200, y: 16, w: 240, h: 34, label: 'Stage', kind: 'stage' }]
    : decorFromLayoutJson(layoutJson);
  return {
    width,
    height,
    decor,
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
  const { user } = useAuth();
  const customerId = user?.userId;
  const isLiveEvent = uuidPattern.test(eventId);
  const mockEvent = isLiveEvent ? null : getMockEvent(eventId);
  const [liveEvent, setLiveEvent] = useState(null);
  const [liveSeats, setLiveSeats] = useState(null);
  const [loading, setLoading] = useState(isLiveEvent);
  const [loadError, setLoadError] = useState('');
  const [selected, setSelected] = useState([]); // mock events only: live events select by holding
  const [hover, setHover] = useState(null);

  // US 7: seats the current customer holds, keyed by seat id -> { seatId, expiresAtMs (server time) }.
  const [holds, setHolds] = useState({});
  const [clockOffset, setClockOffset] = useState(0); // server time minus this device's time, in ms
  const [now, setNow] = useState(() => Date.now());
  const [pending, setPending] = useState({});
  const [notice, setNotice] = useState('');
  const mutationRef = useRef(0); // bumped around every hold/release so stale polls can be ignored
  const eventIdRef = useRef(eventId);

  useEffect(() => { eventIdRef.current = eventId; }, [eventId]);

  const syncClock = useCallback((serverTime) => {
    const serverMs = Date.parse(serverTime);
    if (!Number.isNaN(serverMs)) {
      setClockOffset(serverMs - Date.now());
      setNow(Date.now());
    }
  }, []);

  const applyMine = useCallback((mine) => {
    if (mine && mine.length > 0) syncClock(mine[0].serverTime);
    setHolds(toHoldMap(mine));
  }, [syncClock]);

  const refreshSeats = useCallback(async () => {
    const requestedEventId = eventId;
    const startedAt = mutationRef.current;
    const [seats, mine] = await Promise.all([
      getEventSeats(requestedEventId),
      customerId ? getMyHolds(requestedEventId, customerId) : [],
    ]);
    // Drop the response if we navigated away, or if a hold/release happened while it was in flight.
    if (eventIdRef.current !== requestedEventId || mutationRef.current !== startedAt) return;
    setLiveSeats(seats || []);
    applyMine(mine);
  }, [eventId, customerId, applyMine]);

  useEffect(() => {
    setSelected([]);
    setHover(null);
    setHolds({});
    setPending({});
    setNotice('');
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
    Promise.all([
      getPublishedEvent(eventId),
      getEventSeats(eventId),
      customerId ? getMyHolds(eventId, customerId) : [],
    ])
        .then(([event, seats, mine]) => {
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
          applyMine(mine);
        })
        .catch((error) => {
          if (active) setLoadError(error.message || 'Unable to load this event');
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    return () => { active = false; };
  }, [eventId, isLiveEvent, customerId, applyMine]);

  useEffect(() => {
    if (!isLiveEvent) return undefined;
    const timer = setInterval(() => { refreshSeats().catch(() => {}); }, POLL_MS);
    return () => clearInterval(timer);
  }, [isLiveEvent, refreshSeats]);

  // Tick once a second, but only while there is something to count down.
  const hasHolds = Object.keys(holds).length > 0;
  useEffect(() => {
    if (!hasHolds) return undefined;
    setNow(Date.now());
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [hasHolds]);

  const event = isLiveEvent ? liveEvent : mockEvent;
  const layout = useMemo(() => {
    if (isLiveEvent) return liveSeats === null ? null : liveLayout(liveSeats, liveEvent?.layoutType, liveEvent?.layoutJson);
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

  // A hold whose time has run out is dropped locally straight away (the server has already stopped
  // honouring it), then the seat map is refreshed so the seat shows as available again.
  useEffect(() => {
    const serverNow = now + clockOffset;
    const expired = Object.keys(holds).filter((id) => holds[id].expiresAtMs <= serverNow);
    if (expired.length === 0) return;
    const labels = expired.map((id) => seatById[id]?.label ?? 'a seat').join(', ');
    setHolds((prev) => expired.reduce(without, prev));
    setNotice(`Your hold on ${labels} expired. Select the seat again to hold it.`);
    refreshSeats().catch(() => {});
  }, [now, clockOffset, holds, seatById, refreshSeats]);

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

  const remainingMs = (seatId) => (holds[seatId] ? holds[seatId].expiresAtMs - (now + clockOffset) : 0);
  const selectedIds = isLiveEvent ? Object.keys(holds) : selected;
  const selectedSeats = selectedIds.map((id) => seatById[id]).filter(Boolean);
  // US 12: student prices are shown to everyone; they only apply at checkout to eligible students.
  const discountPercent = isLiveEvent ? (event.studentDiscountPercent || 0) : 0;
  const student = isStudent(user);
  const studentPriceApplies = discountPercent > 0 && student;
  // University-specific events are students-only; the server refuses holds from anyone else too.
  const studentsOnlyBlocked = isLiveEvent && Boolean(event.universitySpecific) && Boolean(user) && !student;
  const pricePaid = (seat) => (studentPriceApplies ? discounted(seat.price, discountPercent) : seat.price);
  const fullTotal = selectedSeats.reduce((sum, s) => sum + s.price, 0);
  const total = Math.round(selectedSeats.reduce((sum, s) => sum + pricePaid(s), 0) * 100) / 100;
  const soonestMs = selectedSeats.length > 0 ? Math.min(...selectedSeats.map((s) => remainingMs(s.id))) : null;

  function toggleMock(seat) {
    if (seat.booked || seat.status === 'BOOKED' || seat.status === 'HELD') return;
    setSelected((prev) => (prev.includes(seat.id) ? prev.filter((id) => id !== seat.id) : [...prev, seat.id]));
  }

  // Selecting a live seat holds it on the server; deselecting releases it.
  async function toggleLive(seat) {
    if (pending[seat.id]) return;
    const isMine = Boolean(holds[seat.id]);
    if (!isMine && seat.status !== 'AVAILABLE') return;
    if (!customerId) {
      setNotice('Please sign in to select seats.');
      return;
    }
    if (studentsOnlyBlocked && !isMine) {
      setNotice('This event is for verified university students only. Verify your student status from your account menu to book.');
      return;
    }

    setNotice('');
    mutationRef.current += 1;
    setPending((prev) => ({ ...prev, [seat.id]: true }));
    let failed = false;
    try {
      if (isMine) {
        await releaseSeatHold(eventId, seat.id, customerId);
        setHolds((prev) => without(prev, seat.id));
        setLiveSeats((prev) => (prev ? prev.map((s) => (s.id === seat.id ? { ...s, status: 'AVAILABLE' } : s)) : prev));
      } else {
        const hold = await holdSeat(eventId, seat.id, customerId);
        syncClock(hold.serverTime);
        setHolds((prev) => ({ ...prev, [seat.id]: toHold(hold) }));
        setLiveSeats((prev) => (prev ? prev.map((s) => (s.id === seat.id ? { ...s, status: 'HELD' } : s)) : prev));
      }
    } catch (error) {
      failed = true;
      setNotice(isMine
          ? `Could not release seat ${seat.label}. ${error.message}`
          : `Seat ${seat.label} could not be held. ${error.message}`);
    } finally {
      mutationRef.current += 1;
      setPending((prev) => without(prev, seat.id));
      if (failed) refreshSeats().catch(() => {}); // show what the seat really looks like now
    }
  }

  function toggle(seat) {
    if (isLiveEvent) {
      toggleLive(seat);
    } else {
      toggleMock(seat);
    }
  }

  function checkout() {
    const order = {
      eventId, isLive: isLiveEvent,
      title: event.title, venue: event.venue, city: event.city, date: event.date, time: event.time,
      seats: selectedSeats.map((s) => ({ id: s.id, label: s.label, tier: s.tier, price: pricePaid(s), fullPrice: s.price })),
      total,
      fullTotal,
      studentDiscountPercent: studentPriceApplies ? discountPercent : null,
    };
    sessionStorage.setItem('pendingOrder', JSON.stringify(order));
    navigate('/checkout');
  }

  const hoverSeat = hover ? seatById[hover] : null;
  const isHeldByMe = (seat) => isLiveEvent && Boolean(holds[seat.id]);
  const unavailable = (seat) => {
    if (isHeldByMe(seat)) return false;
    if (seat.status) return seat.status !== 'AVAILABLE';
    return Boolean(seat.booked);
  };
  const statusText = (seat) => {
    if (isHeldByMe(seat)) return ' · Held for you';
    if (seat.status && seat.status !== 'AVAILABLE') return ` · ${statusName[seat.status] || seat.status}`;
    return seat.booked ? ' · Already booked' : '';
  };

  return (
      <div className="ed">
        <a className="ed-back" href="/browse" onClick={(e) => { e.preventDefault(); navigate('/browse'); }}>← Back to events</a>

        <header className="ed-head">
          <span className="ed-cat">{labelFor[event.cat]}</span>
          <h1>{event.title}</h1>
          <p className="ed-meta">{event.venue}, {event.city} · {event.date} · {event.time}</p>
          {(event.universitySpecific || discountPercent > 0) && (
              <div className="ed-student">
                {event.universitySpecific && (
                    <span className="ed-student-tag">
                      🎓 University students only
                      {studentsOnlyBlocked && ' · you need to be a verified student to book, so you can’t book this event yet. Verify your student status from your account menu.'}
                    </span>
                )}
                {discountPercent > 0 && (
                    <span>
                      Students save <strong>{discountPercent}%</strong>
                      {studentPriceApplies
                          ? ' · you’re a verified student, so student prices apply.'
                          : ' · verify your student status from your account menu to get student prices.'}
                    </span>
                )}
              </div>
          )}
        </header>

        {loadError && <p className="ed-missing" role="alert">{loadError}</p>}
        {notice && <p className="ed-notice" role="alert">{notice}</p>}

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
                      const isSel = isLiveEvent ? isHeldByMe(s) : selected.includes(s.id);
                      const isUnavailable = unavailable(s);
                      let cls = `ed-seat tier-${s.tier}`;
                      if (s.status === 'BOOKED' || s.booked) cls = 'ed-seat is-booked';
                      else if (isSel) cls = 'ed-seat is-selected';
                      else if (s.status === 'HELD') cls = 'ed-seat is-held';
                      if (pending[s.id]) cls += ' is-pending';
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
              {['vip', 'premium', 'standard'].filter((t) => tiers[t] !== undefined).map((t) => (
                  <span key={t}>
                    <i className={`sw tier-${t}`} />{tierName[t]} ${tiers[t]}
                    {discountPercent > 0 && <em className="ed-legend-student">(students {money(discounted(tiers[t], discountPercent))})</em>}
                  </span>
              ))}
              <span><i className="sw is-selected" />{isLiveEvent ? 'Held for you' : 'Selected'}</span>
              <span><i className="sw is-held" />{isLiveEvent ? 'Held by someone else' : 'Held'}</span>
              <span><i className="sw is-booked" />Booked</span>
            </div>
          </div>

          <aside className="ed-cart">
            <h2>Your selection</h2>
            {isLiveEvent && soonestMs !== null && (
                <p className={`ed-hold-banner${soonestMs <= URGENT_MS ? ' is-urgent' : ''}`} role="timer">
                  Seats held for you · <strong>{formatCountdown(soonestMs)}</strong> left
                </p>
            )}
            {selectedSeats.length === 0 ? (
                <p className="ed-empty">No seats selected yet. Pick seats from the plan on the left.</p>
            ) : (
                <ul className="ed-list">
                  {selectedSeats.map((s) => (
                      <li key={s.id}>
                        <span className="ed-li-seat"><strong>Seat {s.label}</strong><small>{tierName[s.tier]}</small></span>
                        {isLiveEvent && (
                            <span
                                className={`ed-li-timer${remainingMs(s.id) <= URGENT_MS ? ' is-urgent' : ''}`}
                                aria-label={`Seat ${s.label} hold time left`}
                            >
                      {formatCountdown(remainingMs(s.id))}
                    </span>
                        )}
                        <span className="ed-li-price">
                          {studentPriceApplies ? (
                              <><s className="ed-was">${s.price}</s> {money(pricePaid(s))}</>
                          ) : `$${s.price}`}
                        </span>
                        <button type="button" className="ed-remove" aria-label={`Remove seat ${s.label}`} onClick={() => toggle(s)}>×</button>
                      </li>
                  ))}
                </ul>
            )}
            {studentPriceApplies && selectedSeats.length > 0 && (
                <div className="ed-discount"><span>Student discount ({discountPercent}%)</span><span>−{money(Math.round((fullTotal - total) * 100) / 100)}</span></div>
            )}
            <div className="ed-total"><span>Total</span><strong>{money(total)}</strong></div>
            <button type="button" className="ed-next" disabled={selectedSeats.length === 0 || studentsOnlyBlocked} onClick={checkout}>
              Continue to payment →
            </button>
            <p className="ed-cart-note">
              {selectedSeats.length} {selectedSeats.length === 1 ? 'seat' : 'seats'} selected
              {isLiveEvent && selectedSeats.length > 0 ? ' · held until the timer runs out' : ''}
            </p>
          </aside>
        </div>
      </div>
  );
}