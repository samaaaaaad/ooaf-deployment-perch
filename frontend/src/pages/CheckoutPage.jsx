import { useMemo } from 'react';

import { useRouter } from '../utils/router';
import './EventDetailPage.css';

const tierName = { vip: 'VIP', premium: 'Premium', standard: 'Standard' };

export default function CheckoutPage() {
  const { navigate } = useRouter();
  const order = useMemo(() => {
    try { return JSON.parse(sessionStorage.getItem('pendingOrder') || 'null'); } catch { return null; }
  }, []);

  if (!order || !order.seats || order.seats.length === 0) {
    return (
      <div className="co">
        <a className="ed-back" href="/browse" onClick={(e) => { e.preventDefault(); navigate('/browse'); }}>← Back to events</a>
        <p className="ed-missing">There is no selection in progress. Pick some seats first.</p>
      </div>
    );
  }

  return (
    <div className="co">
      <a className="ed-back" href="/browse" onClick={(e) => { e.preventDefault(); navigate('/browse'); }}>← Back to events</a>
      <h1 className="co-title">Checkout</h1>

      <div className="co-grid">
        <div className="co-summary">
          <p className="co-eyebrow">Order summary</p>
          <h2>{order.title}</h2>
          <p className="ed-meta">{order.venue}, {order.city} · {order.date} · {order.time}</p>
          <ul className="co-seats">
            {order.seats.map((s) => (
              <li key={s.label}>
                <span><strong>Seat {s.label}</strong><small>{tierName[s.tier]}</small></span>
                <span className="ed-li-price">${s.price}</span>
              </li>
            ))}
          </ul>
          <div className="ed-total"><span>Total</span><strong>${order.total}</strong></div>
        </div>

        <aside className="co-pay">
          <p className="co-eyebrow">Payment</p>
          <p className="co-note">Secure payment is coming soon. This is a placeholder checkout for the customer flow, so no card is taken and no booking is made yet.</p>
          <button type="button" className="ed-next" disabled>Pay ${order.total} · coming soon</button>
          <button type="button" className="co-back-btn" onClick={() => navigate('/browse')}>Keep browsing</button>
        </aside>
      </div>
    </div>
  );
}
