import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import * as eventApi from '../api/eventApi';
import * as venueApi from '../api/venueApi';
import { AuthProvider } from '../auth/AuthContext';
import { RouterProvider } from '../utils/router';
import DashboardPage from './DashboardPage';

vi.mock('../api/eventApi');
vi.mock('../api/venueApi');

describe('DashboardPage', () => {
  beforeEach(() => {
    localStorage.setItem('currentUser', JSON.stringify({
      userId: 'owner-1',
      fullName: 'Jose Mosciaro',
      email: 'jose@example.com',
    }));
  });

  test('loads Event and Venue summaries from the backend APIs', async () => {
    eventApi.getEvents.mockResolvedValue([
      { eventId: 'e1', title: 'Circus', status: 'DRAFT', createdAt: '2026-08-18T00:00:00Z' },
      { eventId: 'e2', title: 'Concert', status: 'PUBLISHED', createdAt: '2026-08-17T00:00:00Z' },
    ]);
    venueApi.getVenues.mockResolvedValue([
      { venueId: 'v1', name: 'Arena', city: 'Sydney', state: 'NSW' },
    ]);

    render(
      <RouterProvider>
        <AuthProvider>
          <DashboardPage />
        </AuthProvider>
      </RouterProvider>,
    );

    expect(await screen.findByText('Circus')).toBeInTheDocument();
    expect(screen.getByText('Concert')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Events 2/ })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Venues 1/ })).toBeInTheDocument();
    expect(eventApi.getEvents).toHaveBeenCalledWith('owner-1');
    expect(venueApi.getVenues).toHaveBeenCalledWith('owner-1');
  });
});
