import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import CheckoutPage, { Confirmation } from './CheckoutPage';
import { createBooking, getPaymentConfig, waitForBookingConfirmation } from '../api/eventApi';
vi.mock('../api/eventApi', () => ({ createBooking: vi.fn(), getPaymentConfig: vi.fn(), waitForBookingConfirmation: vi.fn() }));
vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ user: { userId: 'customer' } }) }));
vi.mock('../utils/router', () => ({ useRouter: () => ({ navigate: vi.fn() }) }));
vi.mock('@stripe/stripe-js', () => ({ loadStripe: vi.fn() }));
vi.mock('@stripe/react-stripe-js', () => ({
  Elements: ({ children }) => children,
  PaymentElement: () => <div>Payment details</div>,
  useStripe: () => ({ confirmPayment: async () => ({ paymentIntent: { status: 'succeeded' } }) }),
  useElements: () => ({}),
}));
beforeEach(() => {
  vi.clearAllMocks();
  sessionStorage.clear();
});

describe('booking confirmation', () => {
  it('shows confirmation and a distinct ticket QR for each seat', () => {
    render(<Confirmation navigate={() => {}} booking={{
      eventTitle: 'Concert', confirmationCode: 'PERCH-123', totalAmount: 40,
      seats: [{ label: 'A1', tier: 'standard', price: 20 }, { label: 'A2', tier: 'standard', price: 20 }],
      tickets: [
        { ticketId: '1', seatLabel: 'A1', ticketCode: 'code-1', qrCodeDataUrl: 'data:image/png;base64,A' },
        { ticketId: '2', seatLabel: 'A2', ticketCode: 'code-2', qrCodeDataUrl: 'data:image/png;base64,B' },
      ],
    }} />);
    expect(screen.getByText('Booking confirmed')).toBeInTheDocument();
    expect(screen.getByText('PERCH-123')).toBeInTheDocument();
    expect(screen.getByText('code-1')).toBeInTheDocument();
    expect(screen.getByText('code-2')).toBeInTheDocument();
    expect(screen.getByAltText('Ticket QR code for seat A1')).toHaveAttribute('src', 'data:image/png;base64,A');
    expect(screen.getByAltText('Ticket QR code for seat A2')).toHaveAttribute('src', 'data:image/png;base64,B');
  });
});

it('retries confirmation without creating another payment after webhook delay', async () => {
  sessionStorage.setItem('pendingOrder', JSON.stringify({
    eventId: 'event', title: 'Concert', total: 20, isLive: true,
    seats: [{ id: 'seat', label: 'A1', tier: 'standard', price: 20 }],
  }));
  createBooking.mockResolvedValue({ status: 'PENDING_PAYMENT', bookingId: 'booking', clientSecret: 'secret' });
  getPaymentConfig.mockResolvedValue({ provider: 'stripe', publishableKey: 'pk_test' });
  waitForBookingConfirmation.mockRejectedValueOnce(new Error('Confirmation delayed')).mockResolvedValueOnce({
    status: 'CONFIRMED', eventTitle: 'Concert', confirmationCode: 'PERCH-OK', totalAmount: 20,
    seats: [{ label: 'A1', tier: 'standard', price: 20 }], tickets: [],
  });
  render(<CheckoutPage />);
  fireEvent.click(screen.getByRole('button', { name: 'Pay $20' }));
  fireEvent.click(await screen.findByRole('button', { name: 'Pay securely' }));
  fireEvent.click(await screen.findByRole('button', { name: 'Check confirmation' }));
  expect(await screen.findByText('Booking confirmed')).toBeInTheDocument();
  expect(createBooking).toHaveBeenCalledTimes(1);
  expect(waitForBookingConfirmation).toHaveBeenCalledTimes(2);
  await waitFor(() => expect(sessionStorage.getItem('pendingOrder')).toBeNull());
});
