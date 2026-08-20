import { useEffect, useRef } from 'react';

import { useRouter } from '../utils/router';
import './LandingPage.css';

/* ---- Small inline icon set (no external dependency) --------------------- */
const icons = {
  grid: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" />
      <rect x="3" y="14" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" />
    </svg>
  ),
  tag: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20.6 13.4 12 22l-9-9V3h10l7.6 7.6a2 2 0 0 1 0 2.8Z" /><circle cx="7.5" cy="7.5" r="1.5" />
    </svg>
  ),
  cursor: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
      <path d="m4 3 7 17 2.5-6.5L20 11 4 3Z" />
    </svg>
  ),
  copy: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
      <rect x="9" y="9" width="12" height="12" rx="2" /><path d="M5 15V5a2 2 0 0 1 2-2h10" />
    </svg>
  ),
  shield: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 2 4 5v6c0 5 3.4 8.5 8 11 4.6-2.5 8-6 8-11V5l-8-3Z" /><path d="m9 12 2 2 4-4" />
    </svg>
  ),
  qr: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" /><path d="M14 14h3v3M21 14v7M17 21h4M14 21v-4" />
    </svg>
  ),
  check: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
      <path d="m5 12 5 5L20 7" />
    </svg>
  ),
};

const features = [
  { key: 'grid', title: 'Drag-and-drop floorplan builder', body: 'Place seats, tables and sections on a grid canvas. Shape any room, from a 40-seat pop-up to a warehouse show.' },
  { key: 'tag', title: 'Independent seat & section pricing', body: 'Price front tables, VIP and standard seats separately, so every seat earns exactly what it is worth.', gold: true },
  { key: 'cursor', title: 'Real-time seat selection', body: 'Customers pick an exact seat and watch availability update live. Temporary holds stop two people grabbing the same spot.' },
  { key: 'copy', title: 'Reusable floorplans', body: 'Build a layout once, save it to your venue and clone it into every future event at that location.' },
  { key: 'shield', title: 'Secure payments', body: 'Tokenised checkout keeps purchases secure. Raw card details are never stored on the platform.' },
  { key: 'qr', title: 'QR e-tickets & check-in', body: 'Confirmed bookings issue scannable e-tickets. Validate entry at the door and block duplicate scans.' },
];

const steps = [
  { title: 'Build your layout', body: 'Drag seats and sections onto the grid until the canvas matches your real room.' },
  { title: 'Price & publish', body: 'Set per-seat or per-section pricing, attach it to an event and go live in minutes.' },
  { title: 'Sell & check in', body: 'Customers reserve seats in real time. You scan their QR tickets at the door.' },
];

const stats = [
  { value: 'Minutes', label: 'From a blank grid to a room you can sell' },
  { value: 'Real time', label: 'Seat availability updates as people choose' },
  { value: 'Any space', label: 'Temporary, mobile or reconfigured venues' },
  { value: 'Zero', label: 'Card details stored on the platform' },
];

/* ---- The floorplan editor illustration (bottom showcase) ---------------- */
function SeatMap() {
  const cols = 9;
  const rows = 5;
  const size = 22;
  const gap = 12;
  const originX = 28;
  const originY = 96;
  const booked = new Set(['1-2', '2-6', '3-1', '4-4', '4-5']);
  const selected = '2-3';

  const seats = [];
  for (let r = 0; r < rows; r += 1) {
    for (let c = 0; c < cols; c += 1) {
      const id = `${r}-${c}`;
      const x = originX + c * (size + gap);
      const y = originY + r * (size + gap);
      let fill = '#2f9488';
      let stroke = 'none';
      let strokeWidth = 0;
      if (r === 0) fill = '#d8a84e';
      if (booked.has(id)) fill = 'rgba(255,255,255,0.14)';
      if (id === selected) { fill = '#ffffff'; stroke = '#d8a84e'; strokeWidth = 2.5; }
      seats.push(
        <rect key={id} x={x} y={y} width={size} height={size} rx="6"
          fill={fill} stroke={stroke} strokeWidth={strokeWidth} />,
      );
    }
  }

  const legend = [
    ['#2f9488', 'Available'],
    ['#d8a84e', 'VIP'],
    ['rgba(255,255,255,0.14)', 'Booked'],
    ['#ffffff', 'Selected'],
  ];

  return (
    <svg className="lp-seatmap-svg" viewBox="0 0 350 300" role="img"
      aria-label="A floorplan editor showing rows of seats with a highlighted VIP row and a selected seat.">
      <rect x="44" y="44" width="262" height="26" rx="13" fill="rgba(255,255,255,0.08)" />
      <text x="175" y="61" textAnchor="middle" fill="rgba(255,255,255,0.55)"
        fontSize="11" fontWeight="700" letterSpacing="3">STAGE</text>

      <rect x="20" y="88" width="310" height="38" rx="10" fill="none"
        stroke="#d8a84e" strokeWidth="1.4" strokeDasharray="5 5" opacity="0.8" />
      <text x="330" y="84" textAnchor="end" fill="#efd49c" fontSize="10" fontWeight="700">VIP · Row A</text>

      {seats}

      <g transform="translate(134 150)">
        <path d="M0 0 L0 17 L4.5 12.5 L7.5 19 L10 18 L7 11.5 L13 11 Z"
          fill="#ffffff" stroke="#0a3b37" strokeWidth="1" />
      </g>

      <g transform="translate(28 268)" fontSize="10" fill="rgba(255,255,255,0.7)" fontWeight="600">
        {legend.map(([color, label], i) => (
          <g key={label} transform={`translate(${i * 78} 0)`}>
            <rect x="0" y="-9" width="12" height="12" rx="3" fill={color}
              stroke={label === 'Selected' ? '#d8a84e' : 'none'} strokeWidth="1.5" />
            <text x="18" y="0">{label}</text>
          </g>
        ))}
      </g>
    </svg>
  );
}

export default function LandingPage() {
  const { navigate } = useRouter();
  const navRef = useRef(null);

  function go(to) {
    return (event) => {
      event.preventDefault();
      navigate(to);
    };
  }

  function scrollTo(id) {
    return (event) => {
      event.preventDefault();
      const el = document.getElementById(id);
      if (!el) return;
      const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      const top = el.getBoundingClientRect().top + window.scrollY - 84;
      window.scrollTo({ top, behavior: reduced ? 'auto' : 'smooth' });
    };
  }

  useEffect(() => {
    document.title = 'Event Ticketing · Reserved seating for pop-ups & mobile venues';

    const onScroll = () => {
      if (navRef.current) navRef.current.classList.toggle('is-stuck', window.scrollY > 12);
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });

    const revealables = Array.from(document.querySelectorAll('.lp-reveal'));
    let observer;
    if ('IntersectionObserver' in window) {
      observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible');
            observer.unobserve(entry.target);
          }
        });
      }, { threshold: 0.12, rootMargin: '0px 0px -8% 0px' });
      revealables.forEach((el) => observer.observe(el));
    } else {
      revealables.forEach((el) => el.classList.add('is-visible'));
    }

    return () => {
      window.removeEventListener('scroll', onScroll);
      if (observer) observer.disconnect();
    };
  }, []);

  return (
    <div className="landing">
      {/* ---- Navigation ---- */}
      <nav className="lp-nav" ref={navRef} aria-label="Primary">
        <div className="lp-shell lp-nav-inner">
          <a className="lp-brand" href="/" onClick={go('/')} aria-label="Event Ticketing home">
            <span className="lp-brand-mark" aria-hidden="true">ET</span>
            <strong>Event Ticketing</strong>
          </a>
          <div className="lp-nav-links">
            <a href="#audiences" onClick={scrollTo('audiences')}>Overview</a>
            <a href="#features" onClick={scrollTo('features')}>Features</a>
            <a href="#how" onClick={scrollTo('how')}>How it works</a>
          </div>
          <div className="lp-nav-actions">
            <a className="lp-nav-login" href="/login" onClick={go('/login')}>Log in</a>
            <a className="lp-btn lp-btn-primary" href="/login" onClick={go('/login?register=1')}>Get started</a>
          </div>
        </div>
      </nav>

      {/* ---- Hero: one-liner + buttons over a live-event photo ---- */}
      <header className="lp-hero">
        <div className="lp-shell lp-hero-inner">
          <p className="lp-eyebrow">Ticketing for pop-ups, promoters and independent venues</p>
          <h1 className="lp-hero-title">Find your seat or <span className="lp-underline">sell out the room</span>.</h1>
          <p className="lp-hero-lead">
            Event Ticketing is where small venues build custom floorplans and event-goers browse,
            pick a seat and check out in real time.
          </p>
          <div className="lp-cta-row">
            <a className="lp-btn lp-btn-primary lp-btn-lg" href="/login" onClick={go('/login')}>
              Browse events
            </a>
            <a className="lp-btn lp-btn-ghost lp-btn-lg" href="/login" onClick={go('/login?register=1')}>
              Sell your event
            </a>
          </div>
          <p className="lp-cta-note">Free to browse · Set up your first event in minutes</p>
        </div>
      </header>

      {/* ---- Stat bar ---- */}
      <section className="lp-stats">
        <div className="lp-shell lp-stats-grid">
          {stats.map((s) => (
            <div className="lp-stat" key={s.label}>
              <strong>{s.value}</strong>
              <span>{s.label}</span>
            </div>
          ))}
        </div>
      </section>

      {/* ---- Two audiences ---- */}
      <section className="lp-section lp-fit" id="audiences">
        <div className="lp-shell">
          <div className="lp-section-head lp-reveal">
            <p className="lp-eyebrow">For organisers and event-goers</p>
            <h2>One platform, two roles</h2>
          </div>
          <div className="lp-audiences">
            <article className="lp-audience lp-reveal">
              <span className="lp-audience-tag">For event-goers</span>
              <h3>Browse events and grab the right seat</h3>
              <ul>
                <li>Browse by category, from cinema and sports to private functions</li>
                <li>See live seat availability and pick the exact seat you want</li>
                <li>Pay securely and get a scannable e-ticket in seconds</li>
              </ul>
            </article>
            <article className="lp-audience lp-audience-gold lp-reveal">
              <span className="lp-audience-tag">For organisers</span>
              <h3>Build the room and sell every seat</h3>
              <ul>
                <li>Design a floorplan for any temporary or mobile space</li>
                <li>Price seats and sections independently, then publish</li>
                <li>Check guests in at the door with a quick QR scan</li>
              </ul>
            </article>
          </div>
        </div>
      </section>

      {/* ---- Features ---- */}
      <section className="lp-section lp-section-alt lp-fit" id="features">
        <div className="lp-shell">
          <div className="lp-section-head lp-reveal">
            <p className="lp-eyebrow">Everything the platform handles</p>
            <h2>From empty room to scanned ticket</h2>
          </div>
          <div className="lp-features">
            {features.map((f) => (
              <article className={`lp-feature lp-reveal${f.gold ? ' lp-feature-gold' : ''}`} key={f.key}>
                <span className="lp-feature-icon" aria-hidden="true">{icons[f.key]}</span>
                <h3>{f.title}</h3>
                <p>{f.body}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      {/* ---- How it works ---- */}
      <section className="lp-section" id="how">
        <div className="lp-shell">
          <div className="lp-section-head lp-reveal">
            <p className="lp-eyebrow">For organisers</p>
            <h2>Set up your event in three steps</h2>
            <p>No bespoke setup and no support tickets to reshape a room.</p>
          </div>
          <div className="lp-steps">
            {steps.map((step, i) => (
              <div className="lp-step lp-reveal" key={step.title}>
                <span className="lp-step-num" aria-hidden="true">{i + 1}</span>
                <h3>{step.title}</h3>
                <p>{step.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ---- Bottom showcase: the seat layout, at the very bottom ---- */}
      <section className="lp-section lp-showcase" id="editor">
        <div className="lp-shell lp-showcase-inner">
          <div className="lp-showcase-copy lp-reveal">
            <p className="lp-eyebrow">The floorplan editor</p>
            <h2>Your seating map, built your way</h2>
            <p>
              Lay out the room exactly as it stands, mark a VIP row and watch seats move between
              available, held and booked in real time. Save it once and reuse it for every event at
              that venue.
            </p>
            <a className="lp-btn lp-btn-primary lp-btn-lg" href="/login" onClick={go('/login?register=1')}>
              Start free
            </a>
          </div>
          <div className="lp-showcase-visual lp-reveal">
            <div className="lp-card">
              <div className="lp-card-bar">
                <span className="lp-dot" style={{ background: '#ff5f57' }} />
                <span className="lp-dot" style={{ background: '#febc2e' }} />
                <span className="lp-dot" style={{ background: '#28c840' }} />
                <span className="lp-card-title">Floorplan editor · Rooftop Session</span>
              </div>
              <SeatMap />
            </div>
          </div>
        </div>
      </section>

      {/* ---- Footer ---- */}
      <footer className="lp-footer">
        <div className="lp-shell">
          <div className="lp-footer-top">
            <div className="lp-footer-brand">
              <a className="lp-brand" href="/" onClick={go('/')}>
                <span className="lp-brand-mark" aria-hidden="true">ET</span>
                <strong style={{ color: '#fff' }}>Event Ticketing</strong>
              </a>
              <p>Reserved-seating ticketing for pop-ups, promoters and independent venues whose room never sits still.</p>
            </div>
            <div className="lp-footer-cols">
              <div className="lp-footer-col">
                <h4>Product</h4>
                <a href="/login" onClick={go('/login')}>Browse events</a>
                <a href="#features" onClick={scrollTo('features')}>Features</a>
                <a href="#editor" onClick={scrollTo('editor')}>Floorplan editor</a>
              </div>
              <div className="lp-footer-col">
                <h4>Account</h4>
                <a href="/login" onClick={go('/login')}>Log in</a>
                <a href="/login" onClick={go('/login?register=1')}>Create account</a>
              </div>
              <div className="lp-footer-col">
                <h4>Project</h4>
                <a href="#how" onClick={scrollTo('how')}>How it works</a>
                <a href="#features" onClick={scrollTo('features')}>ELEC5619 MVP</a>
              </div>
            </div>
          </div>
          <div className="lp-footer-bottom">
            <p>© 2026 Event Ticketing · ELEC5619 Practical 04 · Group 1</p>
            <p>Hero photo via Unsplash · Object-Oriented Application Frameworks</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
