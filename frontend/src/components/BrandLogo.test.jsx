import { render, screen, within } from '@testing-library/react';
import { describe, expect, test, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import BrandLogo from './BrandLogo';
import AppShell from './AppShell';
import LandingPage from '../pages/LandingPage';
import AuthPage from '../pages/AuthPage';
import { RouterProvider } from '../utils/router';

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    user: { userId: 'admin-1', role: 'ADMIN', fullName: 'Admin' },
    signIn: vi.fn(),
    signOut: vi.fn(),
  }),
}));

describe('Perch branding', () => {
  test('uses the primary supplied artwork on light surfaces', () => {
    render(<BrandLogo />);
    expect(screen.getByRole('img', { name: 'Perch' }))
      .toHaveAttribute('src', '/branding/perch-logo-primary.png');
  });

  test('uses the supplied yellow artwork on dark surfaces', () => {
    render(<BrandLogo onDark />);
    expect(screen.getByRole('img', { name: 'Perch' }))
      .toHaveAttribute('src', '/branding/perch-logo-dark-backgrounds.png');
  });

  test('keeps both landing-page logos yellow and both light-page logos primary', () => {
    const landing = render(<RouterProvider><LandingPage /></RouterProvider>);
    const logos = screen.getAllByRole('img', { name: 'Perch' });
    expect(logos).toHaveLength(2);
    logos.forEach((logo) => expect(logo).toHaveAttribute('src', '/branding/perch-logo-dark-backgrounds.png'));
    landing.unmount();

    const auth = render(<RouterProvider><AuthPage /></RouterProvider>);
    expect(screen.getByRole('img', { name: 'Perch' })).toHaveAttribute('src', '/branding/perch-logo-primary.png');
    auth.unmount();

    render(<RouterProvider><AppShell><p>Page content</p></AppShell></RouterProvider>);
    const home = screen.getByRole('link', { name: 'Perch home' });
    expect(within(home).getByRole('img', { name: 'Perch' }))
      .toHaveAttribute('src', '/branding/perch-logo-primary.png');
  });

  test('uses the app icon for the tab, Apple home screen and web manifest', () => {
    const html = readFileSync(resolve('index.html'), 'utf8');
    const head = new DOMParser().parseFromString(html, 'text/html').head;
    expect(head.querySelector('link[rel="icon"]').getAttribute('href')).toBe('/branding/perch-app-icon.png');
    expect(head.querySelector('link[rel="apple-touch-icon"]').getAttribute('href')).toBe('/branding/perch-app-icon.png');
    const manifest = JSON.parse(readFileSync(resolve('public/site.webmanifest'), 'utf8'));
    expect(manifest.icons).toContainEqual(expect.objectContaining({
      src: '/branding/perch-app-icon.png', sizes: '1024x1024', type: 'image/png',
    }));
  });

  test('describes implemented ticket delivery without advertising a check-in scanner', () => {
    render(<RouterProvider><LandingPage /></RouterProvider>);
    expect(screen.getByRole('heading', { name: 'From empty room to confirmed ticket' })).toBeInTheDocument();
    expect(screen.queryByText(/scan their QR tickets at the door|check guests in|sell & check in/i)).not.toBeInTheDocument();
  });
});
