import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import * as adminApi from '../api/adminApi';
import * as organiserRequestApi from '../api/organiserRequestApi';
import * as studentVerificationApi from '../api/studentVerificationApi';
import { AuthProvider } from '../auth/AuthContext';
import AdminDashboardPage from '../pages/AdminDashboardPage';
import { RouterProvider } from '../utils/router';
import AppShell from './AppShell';

vi.mock('../api/adminApi');
vi.mock('../api/organiserRequestApi');
vi.mock('../api/studentVerificationApi');

const CARD = 'data:image/png;base64,iVBORw0KGgo=';

function renderShell() {
  return render(
    <RouterProvider>
      <AuthProvider>
        <AppShell><p>page</p></AppShell>
      </AuthProvider>
    </RouterProvider>,
  );
}

function storeUser(overrides = {}) {
  localStorage.setItem('currentUser', JSON.stringify({
    userId: 'u1', fullName: 'Sam Student', email: 'sam@student.uts.edu.au', role: 'CUSTOMER',
    studentEligible: false, universityEmail: true, ...overrides,
  }));
}

describe('student verification (US 12)', () => {
  beforeEach(() => {
    organiserRequestApi.getLatestOrganiserRequest.mockResolvedValue(null);
    studentVerificationApi.getLatestStudentVerification.mockResolvedValue(null);
    studentVerificationApi.acknowledgeStudentVerification.mockResolvedValue(null);
  });

  test('a customer with a university email applies with their details and card photo', async () => {
    storeUser();
    studentVerificationApi.submitStudentVerification.mockResolvedValue({ requestId: 'r1', status: 'PENDING' });
    renderShell();

    fireEvent.click(screen.getByRole('button', { name: 'Account menu' }));
    fireEvent.click(screen.getByRole('button', { name: 'Verify student status' }));
    fireEvent.change(screen.getByLabelText('University'), { target: { value: 'UTS' } });
    fireEvent.change(screen.getByLabelText('Student number'), { target: { value: '12345678' } });
    const file = new File(['card'], 'card.png', { type: 'image/png' });
    fireEvent.change(screen.getByLabelText('Student card photo'), { target: { files: [file] } });
    await screen.findByAltText('Student card preview');
    fireEvent.click(screen.getByRole('button', { name: 'Send for review' }));

    await waitFor(() => {
      expect(studentVerificationApi.submitStudentVerification).toHaveBeenCalledWith('u1', expect.objectContaining({
        university: 'UTS', studentNumber: '12345678', cardImage: expect.stringMatching(/^data:image\/png/),
      }));
    });
    fireEvent.click(screen.getByRole('button', { name: 'Account menu' }));
    expect(screen.getByText('Student verification pending')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel student verification' })).toBeInTheDocument();
  });

  test('without a university email the customer is told they cannot apply', () => {
    storeUser({ email: 'sam@gmail.com', universityEmail: false });
    renderShell();

    fireEvent.click(screen.getByRole('button', { name: 'Account menu' }));
    fireEvent.click(screen.getByRole('button', { name: 'Verify student status' }));

    expect(screen.getByText(/only available to accounts registered with a university email/)).toBeInTheDocument();
    expect(screen.queryByLabelText('Student number')).not.toBeInTheDocument();
  });

  test('an approval is announced once and unlocks student status', async () => {
    storeUser();
    studentVerificationApi.getLatestStudentVerification.mockResolvedValue({ requestId: 'r1', status: 'APPROVED', notified: false });
    renderShell();

    expect(await screen.findByText(/Your student status has been verified/)).toBeInTheDocument();
    expect(studentVerificationApi.acknowledgeStudentVerification).toHaveBeenCalledWith('u1', 'r1');
    expect(JSON.parse(localStorage.getItem('currentUser')).studentEligible).toBe(true);
    fireEvent.click(screen.getByRole('button', { name: 'Account menu' }));
    expect(screen.getByText('🎓 Verified student')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Verify student status' })).not.toBeInTheDocument();
  });

  test('an admin reviews the card and approves the request', async () => {
    adminApi.getStats.mockResolvedValue({ totalRevenue: 0, popularEventsByTicketsSold: [] });
    adminApi.getPendingOrganiserRequests.mockResolvedValue([]);
    adminApi.getPendingStudentVerifications.mockResolvedValue([{
      requestId: 'r1', userFullName: 'Sam Student', userEmail: 'sam@student.uts.edu.au',
      university: 'UTS', studentNumber: '12345678', cardImage: CARD, createdAt: '2026-10-10T00:00:00Z',
    }]);
    adminApi.approveStudentVerification.mockResolvedValue({ requestId: 'r1', status: 'APPROVED' });
    render(<AdminDashboardPage />);

    const row = (await screen.findByText('12345678')).closest('tr');
    fireEvent.click(within(row).getByRole('button', { name: /View student card/ }));
    expect(screen.getByRole('dialog', { name: /Student card for Sam Student/ })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Close' }));

    fireEvent.click(within(row).getByRole('button', { name: 'Approve' }));
    fireEvent.click(screen.getAllByRole('button', { name: 'Approve' }).at(-1));

    await waitFor(() => expect(adminApi.approveStudentVerification).toHaveBeenCalledWith('r1'));
    expect(await screen.findByText('No pending verifications')).toBeInTheDocument();
  });
});
