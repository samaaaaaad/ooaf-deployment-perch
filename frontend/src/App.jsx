import { useAuth } from './auth/AuthContext';
import AppShell from './components/AppShell';
import AuthPage from './pages/AuthPage';
import BrowseEventsPage from './pages/BrowseEventsPage';
import EventsPage from './pages/EventsPage';
import LandingPage from './pages/LandingPage';
import VenuesPage from './pages/VenuesPage';
import { Redirect, useRouter } from './utils/router';

export default function App() {
  const { user } = useAuth();
  const { path } = useRouter();

  if (!user) {
    if (path === '/login') return <AuthPage />;
    if (path === '/') return <LandingPage />;
    return <Redirect to="/" />;
  }

  // Browse events is the main post-login screen. `/dashboard` is kept as a
  // redirect so the existing login flow still lands somewhere valid.
  if (path === '/login' || path === '/' || path === '/dashboard') {
    return <Redirect to="/browse" />;
  }

  const pages = {
    '/browse': <BrowseEventsPage />,
    '/events': <EventsPage />,
    '/venues': <VenuesPage />,
  };

  return pages[path]
    ? <AppShell>{pages[path]}</AppShell>
    : <Redirect to="/browse" />;
}
