import { useAuth } from './auth/AuthContext';
import AppShell from './components/AppShell';
import AuthPage from './pages/AuthPage';
import DashboardPage from './pages/DashboardPage';
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

  if (path === '/login' || path === '/') {
    return <Redirect to="/dashboard" />;
  }

  const pages = {
    '/dashboard': <DashboardPage />,
    '/events': <EventsPage />,
    '/venues': <VenuesPage />,
  };

  return pages[path]
    ? <AppShell>{pages[path]}</AppShell>
    : <Redirect to="/dashboard" />;
}
