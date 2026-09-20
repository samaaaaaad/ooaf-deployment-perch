import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';

import * as eventApi from '../api/eventApi';
import { AuthProvider } from '../auth/AuthContext';
import { RouterProvider } from '../utils/router';
import EventDetailPage from './EventDetailPage';

vi.mock('../api/eventApi');

const eventId = '80c3fcd5-c837-41df-9a0e-b447f6d4c528';
const customerId = 'customer-1';

const seats = [
  { id: 'seat-a1', label: 'A1', tier: 'PREMIUM', price: 40, x: 182, y: 84, status: 'BOOKED' },
  { id: 'seat-a2', label: 'A2', tier: 'PREMIUM', price: 40, x: 208, y: 84, status: 'HELD' },
  { id: 'seat-c1', label: 'C1', tier: 'STANDARD', price: 25, x: 182, y: 144, status: 'AVAILABLE' },
];

function holdResponse(seatId, label, secondsLeft) {
  const serverTime = Date.now();
  return {
    seatId,
    label,
    status: 'HELD',
    holdExpiresAt: new Date(serverTime + secondsLeft * 1000).toISOString(),
    serverTime: new Date(serverTime).toISOString(),
  };
}

function renderPage() {
  return render(
      <RouterProvider>
        <AuthProvider>
          <EventDetailPage eventId={eventId} />
        </AuthProvider>
      </RouterProvider>,
  );
}

beforeEach(() => {
  localStorage.setItem('currentUser', JSON.stringify({
    userId: customerId,
    fullName: 'Ada Lovelace',
    email: 'ada@example.com',
    role: 'CUSTOMER',
  }));
  eventApi.getPublishedEvent.mockResolvedValue({
    eventId, title: 'US6 Stage Seat Map', layoutType: 'STAGE', standardPrice: 25, premiumPrice: 40,
  });
  eventApi.getEventSeats.mockResolvedValue(seats);
  eventApi.getMyHolds.mockResolvedValue([]);
  eventApi.releaseSeatHold.mockResolvedValue(null);
});

describe('EventDetailPage live seats', () => {
  test('renders live seats from the API and shows category and price', async () => {
    renderPage();

    expect(await screen.findByRole('button', { name: /Seat A1, Premium, \$40 · Already booked/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Seat A2, Premium, \$40 · Held/i })).toBeInTheDocument();
    const available = screen.getByRole('button', { name: /Seat C1, Standard, \$25/i });
    fireEvent.mouseEnter(available);
    expect(screen.getByText(/Seat C1 · Standard · \$25/)).toBeInTheDocument();
  });

  test('selecting an available seat holds it and starts a countdown', async () => {
    eventApi.holdSeat.mockResolvedValue(holdResponse('seat-c1', 'C1', 300));
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: /Seat C1, Standard, \$25/i }));

    expect(await screen.findByText('Seat C1')).toBeInTheDocument();
    expect(eventApi.holdSeat).toHaveBeenCalledWith(eventId, 'seat-c1', customerId);
    expect(screen.getByRole('timer')).toHaveTextContent(/5:00|4:59/);
    expect(screen.getByRole('button', { name: /Seat C1, Standard, \$25 · Held for you, selected/i })).toBeInTheDocument();
  });

  test('a seat someone else holds or has booked cannot be selected', async () => {
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: /Seat A2, Premium/i }));
    fireEvent.click(screen.getByRole('button', { name: /Seat A1, Premium/i }));

    expect(eventApi.holdSeat).not.toHaveBeenCalled();
  });

  test('shows the server message and adds nothing when the seat was just taken', async () => {
    eventApi.holdSeat.mockRejectedValue(new Error('Seat is currently held by another customer'));
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: /Seat C1, Standard, \$25/i }));

    expect(await screen.findByText(/Seat C1 could not be held\. Seat is currently held by another customer/)).toBeInTheDocument();
    expect(screen.getByText(/No seats selected yet/)).toBeInTheDocument();
  });

  test('removing a held seat releases the hold', async () => {
    eventApi.holdSeat.mockResolvedValue(holdResponse('seat-c1', 'C1', 300));
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: /Seat C1, Standard, \$25/i }));
    await screen.findByText('Seat C1');

    fireEvent.click(screen.getByRole('button', { name: 'Remove seat C1' }));

    await waitFor(() => expect(screen.getByText(/No seats selected yet/)).toBeInTheDocument());
    expect(eventApi.releaseSeatHold).toHaveBeenCalledWith(eventId, 'seat-c1', customerId);
  });

  test('restores the customer\'s own holds after a reload', async () => {
    eventApi.getMyHolds.mockResolvedValue([holdResponse('seat-a2', 'A2', 120)]);
    renderPage();

    expect(await screen.findByRole('button', { name: /Seat A2, Premium, \$40 · Held for you, selected/i })).toBeInTheDocument();
    expect(screen.getByText('Seat A2')).toBeInTheDocument();
    expect(screen.getByRole('timer')).toHaveTextContent(/2:00|1:59/);
  });
});

describe('EventDetailPage hold expiry', () => {
  beforeEach(() => { vi.useFakeTimers({ shouldAdvanceTime: true }); });
  afterEach(() => { vi.useRealTimers(); });

  test('drops a hold when its countdown reaches zero and tells the customer', async () => {
    eventApi.holdSeat.mockResolvedValue(holdResponse('seat-c1', 'C1', 3));
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: /Seat C1, Standard, \$25/i }));
    await screen.findByText('Seat C1');

    await act(async () => { await vi.advanceTimersByTimeAsync(3500); });

    expect(await screen.findByText(/Your hold on C1 expired/)).toBeInTheDocument();
    expect(screen.getByText(/No seats selected yet/)).toBeInTheDocument();
  });

  test('switches the banner to the urgent style in the last minute', async () => {
    eventApi.holdSeat.mockResolvedValue(holdResponse('seat-c1', 'C1', 45));
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: /Seat C1, Standard, \$25/i }));

    const timer = await screen.findByRole('timer');
    expect(timer).toHaveClass('is-urgent');
  });
});