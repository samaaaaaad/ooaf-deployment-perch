import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';

import * as venueApi from '../api/venueApi';
import { AuthProvider } from '../auth/AuthContext';
import { RouterProvider } from '../utils/router';
import VenuesPage from './VenuesPage';

vi.mock('../api/venueApi');

const organiserId = 'owner-1';
const basement = {
  venueId: 'v1', organiserId, name: 'Basement Bar', address: '7 Macquarie Pl', city: 'Sydney', state: 'NSW',
  createdAt: '2026-10-01T00:00:00Z',
};

function renderPage(role = 'ORGANISER') {
  localStorage.setItem('currentUser', JSON.stringify({ userId: organiserId, fullName: 'Olive Organiser', email: 'o@example.com', role }));
  return render(
    <RouterProvider>
      <AuthProvider>
        <VenuesPage />
      </AuthProvider>
    </RouterProvider>,
  );
}

function fill(values) {
  Object.entries(values).forEach(([label, value]) => {
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
  });
}

beforeEach(() => {
  venueApi.getVenues.mockResolvedValue([]);
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('VenuesPage', () => {
  test('adds a venue and shows it in the list', async () => {
    venueApi.createVenue.mockResolvedValue(basement);
    renderPage();
    await screen.findByText('No venues yet');

    fill({ 'Venue name': ' Basement Bar ', 'Street address': '7 Macquarie Pl', 'Suburb or city': 'Sydney', State: 'NSW' });
    fireEvent.click(screen.getByRole('button', { name: 'Add venue' }));

    await waitFor(() => expect(venueApi.createVenue).toHaveBeenCalledWith(organiserId, {
      name: 'Basement Bar', address: '7 Macquarie Pl', city: 'Sydney', state: 'NSW',
    }));
    expect(await screen.findByRole('heading', { name: 'Basement Bar' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Basement Bar was added.');
  });

  test('flags a missing field without calling the API', async () => {
    renderPage();
    await screen.findByText('No venues yet');

    fill({ 'Venue name': 'Basement Bar', 'Street address': '7 Macquarie Pl' });
    fireEvent.click(screen.getByRole('button', { name: 'Add venue' }));

    expect(screen.getByRole('alert')).toHaveTextContent('Suburb or city is required');
    expect(venueApi.createVenue).not.toHaveBeenCalled();
  });

  test('shows the server message when saving fails', async () => {
    venueApi.createVenue.mockRejectedValue(new Error('Only organisers can add venues'));
    renderPage();
    await screen.findByText('No venues yet');

    fill({ 'Venue name': 'Hall', 'Street address': '1 Main St', 'Suburb or city': 'Glebe', State: 'NSW' });
    fireEvent.click(screen.getByRole('button', { name: 'Add venue' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Only organisers can add venues');
  });

  test('edits a venue in place', async () => {
    venueApi.getVenues.mockResolvedValue([basement]);
    venueApi.updateVenue.mockResolvedValue({ ...basement, name: 'Basement Bar Upstairs' });
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Edit' }));
    expect(screen.getByLabelText('Venue name')).toHaveValue('Basement Bar');
    fill({ 'Venue name': 'Basement Bar Upstairs' });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => expect(venueApi.updateVenue).toHaveBeenCalledWith(organiserId, 'v1', expect.objectContaining({ name: 'Basement Bar Upstairs' })));
    expect(await screen.findByRole('heading', { name: 'Basement Bar Upstairs' })).toBeInTheDocument();
  });

  test('deletes a venue after confirmation', async () => {
    venueApi.getVenues.mockResolvedValue([basement]);
    venueApi.deleteVenue.mockResolvedValue(null);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderPage();

    const card = (await screen.findByRole('heading', { name: 'Basement Bar' })).closest('article');
    fireEvent.click(within(card).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(venueApi.deleteVenue).toHaveBeenCalledWith(organiserId, 'v1'));
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Basement Bar' })).not.toBeInTheDocument());
  });

  test('is read-only for a pending organiser', async () => {
    venueApi.getVenues.mockResolvedValue([basement]);
    renderPage('PENDING_ORGANISER');

    expect(await screen.findByRole('heading', { name: 'Basement Bar' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add venue' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument();
  });

  test('shows an error when venues cannot be loaded', async () => {
    venueApi.getVenues.mockRejectedValue(new Error('Unable to reach the server. Check your connection and try again.'));
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the server');
  });
});
