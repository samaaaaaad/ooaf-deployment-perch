import { useAuth } from '../auth/AuthContext';
import { Link, NavLink, useRouter } from '../utils/router';

function initials(name) {
  return (name || 'User')
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();
}

export default function AppShell({ children }) {
  const { user, signOut } = useAuth();
  const { navigate } = useRouter();

  function handleLogout() {
    signOut();
    navigate('/login', { replace: true });
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <Link className="brand" to="/browse" aria-label="Event Ticketing home">
          <span className="brand-mark" aria-hidden="true">ET</span>
          <span>
            <strong>Event Ticketing</strong>
            <small>Reserved seating</small>
          </span>
        </Link>

        <nav className="primary-nav" aria-label="Primary navigation">
          <NavLink to="/browse">Browse events</NavLink>
          <NavLink to="/events">My events</NavLink>
          <NavLink to="/venues">Venues</NavLink>
        </nav>

        <div className="account-menu">
          <span className="avatar" aria-hidden="true">{initials(user.fullName)}</span>
          <span className="account-copy">
            <strong>{user.fullName}</strong>
            <small>{user.email}</small>
          </span>
          <button className="button button-secondary button-small" type="button" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </header>

      <main className="page-container">
        {children}
      </main>
    </div>
  );
}
