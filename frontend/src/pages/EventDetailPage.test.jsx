import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import * as eventApi from '../api/eventApi';
import { AuthProvider } from '../auth/AuthContext';
import { RouterProvider } from '../utils/router';
import EventDetailPage from './EventDetailPage';

vi.mock('../api/eventApi');

function renderPage(eventId) {
  return render(
    <RouterProvider>
      <AuthProvider>
        <EventDetailPage eventId={eventId} />
      </AuthProvider>
    </RouterProvider>,
  );
}

describe('EventDetailPage live seats', () => {
  beforeEach(() => {
    localStorage.setItem('currentUser', JSON.stringify({
      userId: 'customer-1',
      fullName: 'Ada Lovelace',
      email: 'ada@example.com',
      role: 'CUSTOMER',
    }));
  });

  test('renders live seats from the API and shows category and price', async () => {
    const eventId = '80c3fcd5-c837-41df-9a0e-b447f6d4c528';
    eventApi.getPublishedEvent.mockResolvedValue({
      eventId, title: 'US6 Stage Seat Map', layoutType: 'STAGE', standardPrice: 25, premiumPrice: 40,
    });
    eventApi.getEventSeats.mockResolvedValue([
      { id: 'seat-a1', label: 'A1', tier: 'PREMIUM', price: 40, x: 182, y: 84, status: 'BOOKED' },
      { id: 'seat-a2', label: 'A2', tier: 'PREMIUM', price: 40, x: 208, y: 84, status: 'HELD' },
      { id: 'seat-c1', label: 'C1', tier: 'STANDARD', price: 25, x: 182, y: 144, status: 'AVAILABLE' },
    ]);

    renderPage(eventId);

    expect(await screen.findByRole('button', { name: /Seat A1, Premium, \$40 · Already booked/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Seat A2, Premium, \$40 · Held/i })).toBeInTheDocument();
    const available = screen.getByRole('button', { name: /Seat C1, Standard, \$25/i });
    fireEvent.mouseEnter(available);
    expect(screen.getByText(/Seat C1 · Standard · \$25/)).toBeInTheDocument();
    fireEvent.click(available);
    expect(screen.getByText('Seat C1')).toBeInTheDocument();
    expect(eventApi.holdSeats).toBeUndefined();
  });
});
