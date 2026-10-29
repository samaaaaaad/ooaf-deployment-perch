import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import * as eventApi from '../api/eventApi';
import { AuthProvider } from '../auth/AuthContext';
import { RouterProvider } from '../utils/router';
import FloorplansPage from './FloorplansPage';

vi.mock('../api/eventApi');

const organiserId = 'organiser-1';

function renderPage() {
    return render(
        <RouterProvider>
            <AuthProvider>
                <FloorplansPage />
            </AuthProvider>
        </RouterProvider>,
    );
}

beforeEach(() => {
    localStorage.setItem('currentUser', JSON.stringify({
        userId: organiserId,
        fullName: 'Ada Lovelace',
        email: 'ada@example.com',
        role: 'ORGANISER',
    }));
    eventApi.getFloorplans.mockResolvedValue([]);
    eventApi.getSharedFloorplans.mockResolvedValue([]);
});

describe('FloorplansPage Community Made section (US 13.0 / 13.4-13.6)', () => {
    test('lists published plans from other organisers, credited to their owner and publish date, separate from "Your floor plans"', async () => {
        eventApi.getFloorplans.mockResolvedValue([
            { floorPlanId: 'plan-mine', organiserId, name: 'My Hall', layoutType: 'CUSTOM', capacity: 10, shared: false, sharedAt: null },
            { floorPlanId: 'plan-default', organiserId: null, name: 'Stage Layout (60 Seats)', layoutType: 'STAGE', capacity: 60, shared: false, sharedAt: null },
        ]);
        eventApi.getSharedFloorplans.mockResolvedValue([
            { floorPlanId: 'plan-theirs', organiserId: 'organiser-2', name: 'Theatre B', layoutType: 'CUSTOM', capacity: 40, ownerName: 'Grace Hopper', shared: true, sharedAt: '2026-10-01T00:00:00Z' },
        ]);

        renderPage();

        await screen.findByText('My Hall');
        expect(await screen.findByText(/40 seats · CUSTOM/)).toBeInTheDocument();
        expect(screen.getByText(new RegExp(`by Grace Hopper · ${new Date('2026-10-01T00:00:00Z').toLocaleDateString()}`))).toBeInTheDocument();
        expect(screen.getByText('Community Made')).toBeInTheDocument();
        // nested directly under the starter templates, not a separate far-away section
        const headings = screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent);
        expect(headings.indexOf('Starter templates')).toBeLessThan(headings.indexOf('Community Made'));
    });

    test("excludes the caller's own published plans from the shared gallery", async () => {
        eventApi.getFloorplans.mockResolvedValue([
            { floorPlanId: 'plan-mine', organiserId, name: 'My Hall', layoutType: 'CUSTOM', capacity: 10, shared: true, sharedAt: '2026-10-01T00:00:00Z' },
        ]);
        eventApi.getSharedFloorplans.mockResolvedValue([
            { floorPlanId: 'plan-mine', organiserId, name: 'My Hall', layoutType: 'CUSTOM', capacity: 10, ownerName: 'Ada Lovelace', shared: true, sharedAt: '2026-10-01T00:00:00Z' },
        ]);

        renderPage();

        await screen.findByText('My Hall');
        expect(screen.getByText('Nothing published yet')).toBeInTheDocument();
        expect(screen.getByText('Published')).toBeInTheDocument(); // the badge on the owner's own card, not a gallery entry
    });

    test('clicking a Community Made card opens a bigger preview showing the owner and publish date', async () => {
        eventApi.getSharedFloorplans.mockResolvedValue([
            { floorPlanId: 'plan-theirs', organiserId: 'organiser-2', name: 'Theatre B', layoutType: 'STAGE', capacity: 60, ownerName: 'Grace Hopper', shared: true, sharedAt: '2026-10-01T00:00:00Z' },
        ]);

        renderPage();
        fireEvent.click(await screen.findByText('Theatre B'));

        expect(await screen.findByRole('heading', { name: 'Theatre B' })).toBeInTheDocument();
        expect(screen.getByText(/Published by Grace Hopper on/)).toBeInTheDocument();
    });

    test('adding a shared plan clones it and shows it under "Your floor plans"', async () => {
        eventApi.getSharedFloorplans.mockResolvedValue([
            { floorPlanId: 'plan-theirs', organiserId: 'organiser-2', name: 'Theatre B', layoutType: 'CUSTOM', capacity: 40, ownerName: 'Grace Hopper', shared: true, sharedAt: '2026-10-01T00:00:00Z' },
        ]);
        eventApi.cloneFloorplan.mockResolvedValue({
            floorPlanId: 'plan-mine-copy', organiserId, name: 'Theatre B (copy)', layoutType: 'CUSTOM', capacity: 40, shared: false, sharedAt: null,
        });

        renderPage();
        fireEvent.click(await screen.findByRole('button', { name: 'Add' }));

        await waitFor(() => expect(eventApi.cloneFloorplan).toHaveBeenCalledWith(organiserId, 'plan-theirs'));
        expect(await screen.findByText('Theatre B (copy)')).toBeInTheDocument();
        expect(screen.getByText(/was added to your floor plans/)).toBeInTheDocument();
    });

    test('shows a server error and keeps the gallery entry when adding fails', async () => {
        eventApi.getSharedFloorplans.mockResolvedValue([
            { floorPlanId: 'plan-theirs', organiserId: 'organiser-2', name: 'Theatre B', layoutType: 'CUSTOM', capacity: 40, ownerName: 'Grace Hopper', shared: true, sharedAt: '2026-10-01T00:00:00Z' },
        ]);
        eventApi.cloneFloorplan.mockRejectedValue(new Error('Only a published floor plan can be added to your own floor plans'));

        renderPage();
        fireEvent.click(await screen.findByRole('button', { name: 'Add' }));

        expect(await screen.findByRole('alert')).toHaveTextContent('Only a published floor plan can be added to your own floor plans');
        expect(screen.getByText('Theatre B')).toBeInTheDocument();
    });
});
