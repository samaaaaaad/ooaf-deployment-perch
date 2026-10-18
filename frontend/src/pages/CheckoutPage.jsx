import BookingTickets from '../components/BookingTickets';
import { useMemo, useState } from 'react';
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js';
import { loadStripe } from '@stripe/stripe-js';

import {
  createBooking,
  getPaymentConfig,
  waitForBookingConfirmation,
} from '../api/eventApi';
import { useAuth } from '../auth/AuthContext';
import { useRouter } from '../utils/router';
import './CheckoutPage.css';

const tierName = { vip: 'VIP', premium: 'Premium', standard: 'Standard' };

const LockIcon = (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="4" y="11" width="16" height="10" rx="2" /><path d="M8 11V7a4 4 0 0 1 8 0v4" />
  </svg>
);
const CardIcon = (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="2" y="5" width="20" height="14" rx="2" /><path d="M2 10h20" />
  </svg>
);
const CheckIcon = (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
    <path d="M20 6 9 17l-5-5" />
  </svg>
);

export default function CheckoutPage() {
  const { navigate } = useRouter();
  const { user } = useAuth();
  const order = useMemo(() => {
    try { return JSON.parse(sessionStorage.getItem('pendingOrder') || 'null'); } catch { return null; }
  }, []);

  const [status, setStatus] = useState('idle');
  const [error, setError] = useState('');
  const [booking, setBooking] = useState(null);
  const [stripeState, setStripeState] = useState(null); // { stripePromise, clientSecret, bookingId }

  const total = order?.total ?? 0;
  const isFree = Number(total) === 0;

  async function handlePay() {
    if (!user?.userId) { setError('Please sign in to complete your booking.'); return; }
    const seatIds = (order.seats || []).map((s) => s.id).filter(Boolean);
    if (seatIds.length === 0) { setError('Your selection is missing seat details. Please reselect your seats.'); return; }
    setStatus('paying');
    setError('');
    try {
      const result = await createBooking(order.eventId, user.userId, seatIds);

      if (result.status === 'CONFIRMED') {
        setBooking(result);
        setStatus('done');
        sessionStorage.removeItem('pendingOrder');
        return;
      }

      if (result.status === 'PENDING_PAYMENT' && result.clientSecret) {
        const config = await getPaymentConfig();
        if (config.provider !== 'stripe' || !config.publishableKey) {
          throw new Error('Stripe is not configured on the server.');
        }
        setStripeState({
          stripePromise: loadStripe(config.publishableKey),
          clientSecret: result.clientSecret,
          bookingId: result.bookingId,
        });
        setStatus('stripe');
        return;
      }

      throw new Error('Unexpected booking response. Please try again.');
    } catch (err) {
      setError(err.message || 'We could not complete your booking.');
      setStatus('error');
    }
  }

  async function onStripeConfirmed() {
    try {
      setStatus('paying');
      const confirmed = await waitForBookingConfirmation(stripeState.bookingId);
      setBooking(confirmed);
      setStatus('done');
      setStripeState(null);
      sessionStorage.removeItem('pendingOrder');
    } catch (err) {
      setError(err.message || 'Payment succeeded but confirmation is delayed.');
      setStatus('awaiting-confirmation');
    }
  }

  if (status === 'done' && booking) {
    return <Confirmation booking={booking} navigate={navigate} />;
  }

  if (!order || !order.seats || order.seats.length === 0) {
    return (
      <div className="cop">
        <a className="cop-back" href="/browse" onClick={(e) => { e.preventDefault(); navigate('/browse'); }}>← Back to events</a>
        <div className="cop-card"><p className="cop-note">There is no selection in progress. Pick some seats first.</p></div>
      </div>
    );
  }

  const canBook = order.isLive;

  return (
    <div className="cop">
      <a className="cop-back" href="/browse" onClick={(e) => { e.preventDefault(); navigate('/browse'); }}>← Back to events</a>

      <div className="cop-steps">
        <span className="cop-step is-done"><span className="cop-num">✓</span>Seats</span>
        <span className="cop-step-line" />
        <span className={`cop-step ${(status === 'stripe' || status === 'paying') ? 'is-active' : status === 'done' ? 'is-done' : 'is-active'}`}>
          <span className="cop-num">2</span>Payment
        </span>
        <span className="cop-step-line" />
        <span className="cop-step"><span className="cop-num">3</span>Confirmation</span>
      </div>

      <h1 className="cop-title">Checkout</h1>

      <div className="cop-grid">
        <div className="cop-card cop-summary">
          <p className="cop-eyebrow">Order summary</p>
          <h2 className="cop-event">{order.title}</h2>
          <p className="cop-meta">{order.venue}, {order.city} · {order.date} · {order.time}</p>
          <ul className="cop-seats">
            {order.seats.map((s) => (
              <li key={s.label}>
                <span className="cop-seat-label"><strong>Seat {s.label}</strong><small>{tierName[s.tier] || s.tier}</small></span>
                <span className="cop-seat-price">
                  {s.fullPrice !== undefined && Number(s.fullPrice) !== Number(s.price) && <s className="cop-was">${s.fullPrice}</s>}
                  {Number(s.price) === 0 ? 'Free' : `$${s.price}`}
                </span>
              </li>
            ))}
          </ul>
          {order.studentDiscountPercent > 0 && (
            <p className="cop-discount">Student discount ({order.studentDiscountPercent}%) applied · you save ${(order.fullTotal - total).toFixed(2)}</p>
          )}
          <div className="cop-total"><span>Total</span><strong>{isFree ? 'Free' : `$${total}`}</strong></div>
        </div>

        <aside className="cop-card cop-pay">
          <div className="cop-pay-head">
            <h2 className="cop-pay-title">{isFree ? 'Reservation' : 'Payment'}</h2>
            {!isFree && canBook && (
              <span className="cop-secure">{LockIcon} Secure checkout</span>
            )}
          </div>

          {!canBook ? (
            <p className="cop-note">This is a sample catalogue event, so it can’t be booked. Pick a live published event to complete a real booking.</p>
          ) : status === 'stripe' && stripeState ? (
            <Elements
              stripe={stripeState.stripePromise}
              options={{ clientSecret: stripeState.clientSecret, appearance: { theme: 'stripe' } }}
            >
              <StripePayForm
                onConfirmed={onStripeConfirmed}
                onError={(message) => { setError(message); setStatus('stripe'); }}
              />
            </Elements>
          ) : isFree ? (
            <p className="cop-note">This event is free, so there’s nothing to pay. Just confirm your reservation and your tickets are yours.</p>
          ) : (
            <>
              <div className="cop-brands">
                <span className="cop-brand">VISA</span>
                <span className="cop-brand">MASTERCARD</span>
                <span className="cop-brand">AMEX</span>
              </div>
              <p className="cop-note">
                Card details are collected securely by Stripe after you click Pay.
                With the mock provider (default), payment confirms instantly with no card entry.
              </p>
              <div className="cop-field" aria-hidden="true">
                <label>Card number</label>
                <div className="cop-input-wrap">
                  {CardIcon}
                  <input type="text" inputMode="numeric" placeholder="4242 4242 4242 4242" autoComplete="off" disabled />
                </div>
              </div>
            </>
          )}

          {error && <div className="notice notice-error" role="alert" style={{ marginTop: '0.5rem', marginBottom: '0.75rem' }}>{error}</div>}

          {status !== 'stripe' && (
            <button type="button" className="cop-pay-btn" disabled={!canBook || status === 'paying'} onClick={status === 'awaiting-confirmation' ? onStripeConfirmed : handlePay}>
              {status === 'paying'
                ? 'Confirming…'
                : status === 'awaiting-confirmation'
                  ? 'Check confirmation'
                : isFree
                  ? 'Reserve for free'
                  : <>{LockIcon} Pay ${total}</>}
            </button>
          )}
          <button type="button" className="cop-ghost" onClick={() => navigate('/browse')}>Keep browsing</button>
        </aside>
      </div>
    </div>
  );
}

function StripePayForm({ onConfirmed, onError }) {
  const stripe = useStripe();
  const elements = useElements();
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!stripe || !elements) return;
    setSubmitting(true);
    const { error, paymentIntent } = await stripe.confirmPayment({
      elements,
      redirect: 'if_required',
    });
    if (error) {
      onError(error.message || 'Payment was declined.');
      setSubmitting(false);
      return;
    }
    if (paymentIntent && (paymentIntent.status === 'succeeded' || paymentIntent.status === 'processing')) {
      await onConfirmed();
      return;
    }
    onError('Payment was not completed. Please try again.');
    setSubmitting(false);
  }

  return (
    <form onSubmit={handleSubmit}>
      <PaymentElement options={{ layout: 'tabs' }} />
      <button type="submit" className="cop-pay-btn" style={{ marginTop: '1rem' }} disabled={!stripe || submitting}>
        {submitting ? 'Confirming…' : <>{LockIcon} Pay securely</>}
      </button>
    </form>
  );
}

export function Confirmation({ booking, navigate }) {
  return (
    <div className="cop">
      <div className="cop-card cop-done">
        <div className="cop-check">{CheckIcon}</div>
        <p className="cop-eyebrow" style={{ color: 'var(--brand, #176b63)' }}>Booking confirmed</p>
        <h1>You’re in!</h1>
        <p className="cop-done-event">{booking.eventTitle}</p>

        <div className="cop-code">
          <span>Confirmation code</span>
          <strong>{booking.confirmationCode}</strong>
        </div>

        <ul className="cop-seats" style={{ textAlign: 'left' }}>
          {booking.seats.map((s) => (
            <li key={s.label}>
              <span className="cop-seat-label"><strong>Seat {s.label}</strong><small>{tierName[s.tier] || s.tier}</small></span>
              <span className="cop-seat-price">{Number(s.price) === 0 ? 'Free' : `$${s.price}`}</span>
            </li>
          ))}
        </ul>
        <div className="cop-total"><span>Total paid</span><strong>{Number(booking.totalAmount) === 0 ? 'Free' : `$${booking.totalAmount}`}</strong></div>

        <p className="cop-note" style={{ marginTop: '1rem' }}>Your tickets are ready below and saved in My Tickets.</p>

        <BookingTickets tickets={booking.tickets} />
        <div className="cop-done-actions">
          <button type="button" className="cop-pay-btn" style={{ flex: 1 }} onClick={() => navigate('/tickets')}>View My Tickets</button>
          <button type="button" className="cop-ghost" style={{ flex: 1 }} onClick={() => navigate('/browse')}>Keep browsing</button>
        </div>
      </div>
    </div>
  );
}
