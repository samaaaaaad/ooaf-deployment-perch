import { useEffect, useState } from 'react';

import { getMyBookings } from '../api/eventApi';
import { useAuth } from '../auth/AuthContext';
import EmptyState from '../components/EmptyState';
import LoadingState from '../components/LoadingState';
import { useRouter } from '../utils/router';

const tierName = { vip: 'VIP', premium: 'Premium', standard: 'Standard' };

export default function MyTicketsPage() {
  const { user } = useAuth();
  const { navigate } = useRouter();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getMyBookings(user.userId)
      .then((data) => { if (active) setBookings(data || []); })
      .catch((err) => { if (active) setError(err.message || 'Unable to load your tickets'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user.userId]);

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Your bookings</p>
          <h1>My Tickets</h1>
          <p>Every seat you’ve booked, with its confirmation code.</p>
        </div>
      </div>

      {error && <div className="notice notice-error" role="alert">{error}</div>}

      {loading ? (
        <LoadingState label="Loading your tickets" />
      ) : bookings.length === 0 ? (
        <div className="content-card">
          <EmptyState
            title="No tickets yet"
            description="Book a seat from an event and it’ll show up here."
          />
          <button type="button" className="button button-primary" onClick={() => navigate('/browse')}>Browse events</button>
        </div>
      ) : (
        <div className="fp-list">
          {bookings.map((b) => (
            <div key={b.bookingId} className="content-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: '1rem' }}>
                <h3 style={{ margin: 0 }}>{b.eventTitle}</h3>
                <span className="status-pill">{b.confirmationCode}</span>
              </div>
              <ul className="co-seats" style={{ marginTop: '0.75rem' }}>
                {b.seats.map((s) => (
                  <li key={s.label}>
                    <span><strong>Seat {s.label}</strong><small>{tierName[s.tier] || s.tier}</small></span>
                    <span className="ed-li-price">{Number(s.price) === 0 ? 'Free' : `$${s.price}`}</span>
                  </li>
                ))}
              </ul>
              <div className="ed-total"><span>Total</span><strong>{Number(b.totalAmount) === 0 ? 'Free' : `$${b.totalAmount}`}</strong></div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
