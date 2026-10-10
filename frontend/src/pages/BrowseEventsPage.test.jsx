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
  { eventId: '33333333-3333-3333-3333-333333333333', title: 'Jazz Night', standardPrice: 50, premiumPrice: 60, universitySpecific: false, studentDiscountPercent: 10 },
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
    expect(within(card).getByText('🎓 Uni')).toBeInTheDocument();
  });
});
