import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import * as authApi from '../api/authApi';
import { AuthProvider } from '../auth/AuthContext';
import { RouterProvider, useRouter } from '../utils/router';
import AuthPage from './AuthPage';

vi.mock('../api/authApi');

function AuthHarness() {
  const { path } = useRouter();
  return path === '/dashboard' ? <h1>Dashboard reached</h1> : <AuthPage />;
}

function renderAuthPage() {
  window.history.replaceState({}, '', '/login');
  return render(
    <RouterProvider>
      <AuthProvider>
        <AuthHarness />
      </AuthProvider>
    </RouterProvider>,
  );
}

describe('AuthPage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  test('preserves the teammate login flow and canonicalizes the email', async () => {
    authApi.login.mockResolvedValue({
      userId: 'user-1',
      fullName: 'Jane Smith',
      email: 'jane@example.com',
      role: 'CUSTOMER',
      isActive: true,
    });
    renderAuthPage();

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: ' Jane@Example.COM ' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'Secure1!' } });
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }));

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({
        email: 'jane@example.com',
        password: 'Secure1!',
      });
    });
    expect(await screen.findByRole('heading', { name: 'Dashboard reached' })).toBeInTheDocument();
    expect(JSON.parse(localStorage.getItem('currentUser')).userId).toBe('user-1');
  });

  test('keeps the original registration password validation', async () => {
    renderAuthPage();
    fireEvent.click(screen.getByRole('button', { name: 'Create an account' }));

    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Jane Smith' } });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'jane@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'weak' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('at least 8 characters');
    expect(authApi.register).not.toHaveBeenCalled();
  });
});
