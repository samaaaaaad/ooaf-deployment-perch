import { useAuth } from './auth/AuthContext';
import AppShell from './components/AppShell';
import AdminDashboardPage from './pages/AdminDashboardPage';
import AuthPage from './pages/AuthPage';
import BrowseEventsPage from './pages/BrowseEventsPage';
import CheckoutPage from './pages/CheckoutPage';
import EventDetailPage from './pages/EventDetailPage';
import EventsPage from './pages/EventsPage';
import FloorPlanEditorPage from './pages/FloorPlanEditorPage';
import FloorplansPage from './pages/FloorplansPage';
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

  // Browse events is the main post-login screen for regular users; admins land
  // on their dashboard instead. `/dashboard` is kept as a redirect so the
  // existing login flow still lands somewhere valid.
  if (path === '/login' || path === '/' || path === '/dashboard') {
    return <Redirect to={user.role === 'ADMIN' ? '/admin' : '/browse'} />;
  }

  if (path.startsWith('/event/')) {
    return <AppShell><EventDetailPage eventId={path.slice('/event/'.length)} /></AppShell>;
  }

  if (path === '/admin' && user.role !== 'ADMIN') {
    return <Redirect to="/browse" />;
  }

  const organiserOnly = ['/events', '/venues', '/floorplan', '/floorplans'];
  if (organiserOnly.includes(path) && user.role !== 'ORGANISER') {
    return <Redirect to="/browse" />;
  }

  const pages = {
    '/browse': <BrowseEventsPage />,
    '/checkout': <CheckoutPage />,
    '/events': <EventsPage />,
    '/venues': <VenuesPage />,
    '/floorplan': <FloorPlanEditorPage />,
    '/floorplans': <FloorplansPage />,
    '/admin': <AdminDashboardPage />,
  };

  return pages[path]
    ? <AppShell>{pages[path]}</AppShell>
    : <Redirect to="/browse" />;
}
