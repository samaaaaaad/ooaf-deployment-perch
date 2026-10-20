import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import * as eventApi from '../api/eventApi';
import { AuthProvider } from '../auth/AuthContext';
import { RouterProvider } from '../utils/router';
import MyTicketsPage from './MyTicketsPage';

vi.mock('../api/eventApi');

function renderPage() {
  return render(
    <RouterProvider>
      <AuthProvider>
        <MyTicketsPage />
      </AuthProvider>
    </RouterProvider>,
  );
}

describe('MyTicketsPage', () => {
  beforeEach(() => {
    localStorage.setItem('currentUser', JSON.stringify({
      userId: 'cust-1',
      fullName: 'Casey Customer',
      email: 'casey@example.com',
      role: 'CUSTOMER',
    }));
  });

  test('shows a booking with its code, seats and e-ticket', async () => {
    eventApi.getMyBookings.mockResolvedValue([
      {
        bookingId: 'b1',
        eventTitle: 'Rooftop Jazz',
        confirmationCode: 'PERCH-7KQ2MN',
        totalAmount: '80.00',
        seats: [{ label: 'A1', tier: 'premium', price: '40.00' }],
        tickets: [{
          ticketId: 't1',
          seatLabel: 'A1',
          ticketCode: 'ab12-code',
          qrCodeDataUrl: 'data:image/png;base64,FAKE',
        }],
      },
    ]);

    renderPage();

    expect(await screen.findByText('Rooftop Jazz')).toBeInTheDocument();
    expect(screen.getByText('PERCH-7KQ2MN')).toBeInTheDocument();
    expect(screen.getByText('Seat A1')).toBeInTheDocument();
    // the e-ticket renders a QR image labelled by its seat
    expect(screen.getByAltText('Ticket QR code for seat A1')).toBeInTheDocument();
    expect(screen.getByText('ab12-code')).toBeInTheDocument();
  });

  test('shows the empty state when there are no bookings', async () => {
    eventApi.getMyBookings.mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText('No tickets yet')).toBeInTheDocument();
  });
});
