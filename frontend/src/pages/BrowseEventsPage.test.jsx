import { fireEvent, render, screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import * as eventApi from '../api/eventApi';
import { AuthProvider } from '../auth/AuthContext';
import { RouterProvider } from '../utils/router';
import BrowseEventsPage from './BrowseEventsPage';

vi.mock('../api/eventApi');

const published = [
  { eventId: '11111111-1111-1111-1111-111111111111', title: 'O-Week Social', standardPrice: 25, premiumPrice: 40, universitySpecific: true, studentDiscountPercent: 20 },
  { eventId: '22222222-2222-2222-2222-222222222222', title: 'Campus Comedy', standardPrice: 30, premiumPrice: 30, universitySpecific: true, studentDiscountPercent: null },
  { eventId: '33333333-3333-3333-3333-333333333333', title: 'Jazz Night', category: 'music', venueName: 'Basement Bar', city: 'Sydney CBD', standardPrice: 50, premiumPrice: 60, universitySpecific: false, studentDiscountPercent: 10 },
];

function renderPage() {
  return render(
    <RouterProvider>
      <AuthProvider>
        <BrowseEventsPage />
      </AuthProvider>
    </RouterProvider>,
  );
}

describe('BrowseEventsPage student filters (US 12)', () => {
  beforeEach(() => {
    localStorage.setItem('currentUser', JSON.stringify({
      userId: 'c1', fullName: 'Sam Student', email: 'sam@student.uts.edu.au', role: 'CUSTOMER', studentEligible: true,
    }));
    eventApi.getPublishedEvents.mockResolvedValue(published);
  });

  test('filters to university events', async () => {
    renderPage();
    await screen.findByText('O-Week Social');

    fireEvent.click(screen.getByRole('button', { name: /University events/ }));

    expect(screen.getByText('O-Week Social')).toBeInTheDocument();
    expect(screen.getByText('Campus Comedy')).toBeInTheDocument();
    expect(screen.queryByText('Jazz Night')).not.toBeInTheDocument();
  });

  test('filters to student-discounted events, and combines with the university filter', async () => {
    renderPage();
    await screen.findByText('O-Week Social');

    fireEvent.click(screen.getByRole('button', { name: 'Student discount' }));
    expect(screen.getByText('Jazz Night')).toBeInTheDocument();
    expect(screen.queryByText('Campus Comedy')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /University events/ }));
    expect(screen.getByText('O-Week Social')).toBeInTheDocument();
    expect(screen.queryByText('Jazz Night')).not.toBeInTheDocument();
  });

  test('shows the standard and the student price on a discounted card', async () => {
    renderPage();
    const card = (await screen.findByText('O-Week Social')).closest('a');

    expect(within(card).getByText('From $25')).toBeInTheDocument();
    expect(within(card).getByText(/\$20 students/)).toBeInTheDocument();
    expect(within(card).getByText(/your price/)).toBeInTheDocument();
    expect(within(card).getByText('🎓 Students only')).toBeInTheDocument();
  });

  test('shows each event\'s stored venue, city and category, and filters by category', async () => {
    renderPage();
    const card = (await screen.findByText('Jazz Night')).closest('a');

    expect(within(card).getByText('Basement Bar, Sydney CBD')).toBeInTheDocument();
    expect(within(card).getByText('Music')).toBeInTheDocument();
    const undated = (await screen.findByText('Campus Comedy')).closest('a');
    expect(within(undated).getByText('Location to be announced')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Music' }));
    expect(screen.getByText('Jazz Night')).toBeInTheDocument();
    expect(screen.queryByText('Campus Comedy')).not.toBeInTheDocument();
  });
});

describe('BrowseEventsPage loading and errors', () => {
  beforeEach(() => {
    localStorage.setItem('currentUser', JSON.stringify({ userId: 'c1', fullName: 'Bea Buyer', email: 'bea@example.com', role: 'CUSTOMER' }));
  });

  test('shows a loading state, then only the events from the API', async () => {
    eventApi.getPublishedEvents.mockResolvedValue(published);
    renderPage();

    expect(screen.getByRole('status')).toHaveTextContent('Loading events…');
    expect(await screen.findByText('3 events')).toBeInTheDocument();
  });

  test('explains when there are no upcoming events', async () => {
    eventApi.getPublishedEvents.mockResolvedValue([]);
    renderPage();

    expect(await screen.findByText('No upcoming events yet')).toBeInTheDocument();
  });

  test('shows the error and retries when the request fails', async () => {
    eventApi.getPublishedEvents
      .mockRejectedValueOnce(new Error('Unable to reach the server. Check your connection and try again.'))
      .mockResolvedValueOnce(published);
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the server');
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByText('Jazz Night')).toBeInTheDocument();
  });
});
