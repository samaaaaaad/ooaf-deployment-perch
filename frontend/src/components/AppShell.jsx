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

  const isOrganiser = user?.role === 'ORGANISER';

  return (
    <div className="app-shell">
      <header className="app-header">
        <Link className="brand" to="/browse" aria-label="Perch home">
          <span className="brand-mark" aria-hidden="true">P</span>
          <span>
            <strong>Perch</strong>
            <small>Reserved seating</small>
          </span>
        </Link>

        <nav className="primary-nav" aria-label="Primary navigation">
          <NavLink to="/browse">Browse</NavLink>
          {isOrganiser && <NavLink to="/events">My Events</NavLink>}
          {isOrganiser && <NavLink to="/venues">Venues</NavLink>}
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
