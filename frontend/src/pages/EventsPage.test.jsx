import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import * as eventApi from '../api/eventApi';
import { AuthProvider } from '../auth/AuthContext';
import { RouterProvider } from '../utils/router';
import EventsPage from './EventsPage';

vi.mock('../api/eventApi');

function renderPage() {
    return render(
        <RouterProvider>
            <AuthProvider>
                <EventsPage />
            </AuthProvider>
        </RouterProvider>,
    );
}

describe('EventsPage', () => {
    describe('as an organiser', () => {
        beforeEach(() => {
            localStorage.setItem('currentUser', JSON.stringify({
                userId: 'owner-1',
                fullName: 'Jose Mosciaro',
                email: 'jose@example.com',
                role: 'ORGANISER',
            }));
        });

        test('shows the create-event form and submits a new event', async () => {
            eventApi.getEvents.mockResolvedValue([]);
            eventApi.createEvent.mockResolvedValue({
                eventId: 'e1',
                title: 'Circus',
                description: 'Big top show',
                status: 'DRAFT',
                createdAt: '2026-08-18T00:00:00Z',
            });

            renderPage();

            expect(await screen.findByText('No events yet')).toBeInTheDocument();

            fireEvent.change(screen.getByLabelText('Title'), { target: { value: 'Circus' } });
            fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'Big top show' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create event' }));

            await waitFor(() => {
                expect(eventApi.createEvent).toHaveBeenCalledWith('owner-1', {
                    title: 'Circus',
                    description: 'Big top show',
                    universitySpecific: false,
                });
            });

            expect(await screen.findByText('Circus')).toBeInTheDocument();
        });

        test('shows a validation message instead of calling the API for a blank title', async () => {
            eventApi.getEvents.mockResolvedValue([]);

            renderPage();
            await screen.findByText('No events yet');

            fireEvent.click(screen.getByRole('button', { name: 'Create event' }));

            expect(await screen.findByText('Event title is required')).toBeInTheDocument();
            expect(eventApi.createEvent).not.toHaveBeenCalled();
        });

        test('surfaces a backend error without clearing the form', async () => {
            eventApi.getEvents.mockResolvedValue([]);
            eventApi.createEvent.mockRejectedValue(new Error('Only organisers can create events'));

            renderPage();
            await screen.findByText('No events yet');

            fireEvent.change(screen.getByLabelText('Title'), { target: { value: 'Circus' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create event' }));

            expect(await screen.findByText('Only organisers can create events')).toBeInTheDocument();
            expect(screen.getByLabelText('Title')).toHaveValue('Circus');
        });
    });

    describe('as a customer', () => {
        beforeEach(() => {
            localStorage.setItem('currentUser', JSON.stringify({
                userId: 'cust-1',
                fullName: 'Alex Customer',
                email: 'alex@example.com',
                role: 'CUSTOMER',
            }));
        });

        test('does not render the create-event form', async () => {
            eventApi.getEvents.mockResolvedValue([]);

            renderPage();

            await screen.findByText('No events yet');
            expect(screen.queryByLabelText('Title')).not.toBeInTheDocument();
            expect(screen.getByText('Read-only')).toBeInTheDocument();
        });
    });
});