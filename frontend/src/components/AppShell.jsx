import { useEffect, useState } from 'react';

import {
  acknowledgeOrganiserRequest,
  cancelOrganiserRequest,
  getLatestOrganiserRequest,
  submitOrganiserRequest,
} from '../api/organiserRequestApi';
import { useAuth } from '../auth/AuthContext';
import { Link, NavLink, useRouter } from '../utils/router';
import ConfirmDialog from './ConfirmDialog';

function initials(name) {
  return (name || 'User')
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();
}

export default function AppShell({ children }) {
  const { user, signIn, signOut } = useAuth();
  const { navigate } = useRouter();
  const [latestRequest, setLatestRequest] = useState(null);
  const [outcomeBanner, setOutcomeBanner] = useState(null);
  const [confirmAction, setConfirmAction] = useState(null); // 'request' | 'cancel'

  function handleLogout() {
    signOut();
    navigate('/login', { replace: true });
  }

  const isOrganiser = user?.role === 'ORGANISER';
  const isPendingOrganiser = user?.role === 'PENDING_ORGANISER';
  const isCustomer = user?.role === 'CUSTOMER';
  const isAdmin = user?.role === 'ADMIN';
  const canViewOrganiserPages = isOrganiser || isPendingOrganiser;

  useEffect(() => {
    if (!user || isAdmin) return;
    let active = true;
    getLatestOrganiserRequest(user.userId)
      .then((latest) => {
        if (!active || !latest) return;
        setLatestRequest(latest);
        if (latest.notified || (latest.status !== 'APPROVED' && latest.status !== 'REJECTED')) return;

        setOutcomeBanner(latest.status);
        const resolvedRole = latest.status === 'APPROVED' ? 'ORGANISER' : 'CUSTOMER';
        if (user.role !== resolvedRole) {
          signIn({ ...user, role: resolvedRole });
        }
        acknowledgeOrganiserRequest(user.userId, latest.requestId).catch(() => {});
      })
      .catch(() => {});
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.userId]);

  async function handleConfirmRequest() {
    const updated = await submitOrganiserRequest(user.userId);
    signIn(updated);
    const latest = await getLatestOrganiserRequest(user.userId);
    setLatestRequest(latest);
    setConfirmAction(null);
  }

  async function handleConfirmCancel() {
    const updated = await cancelOrganiserRequest(user.userId, latestRequest.requestId);
    signIn(updated);
    setLatestRequest(null);
    setConfirmAction(null);
  }

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
          {isAdmin && <NavLink to="/admin">Dashboard</NavLink>}
          {canViewOrganiserPages && <NavLink to="/events">My Events</NavLink>}
          {canViewOrganiserPages && <NavLink to="/floorplans">Floor Plans</NavLink>}
          {canViewOrganiserPages && <NavLink to="/venues">Venues</NavLink>}
        </nav>

        <div className="account-menu">
          {isCustomer && (
            <button
              className="button button-secondary button-small"
              type="button"
              onClick={() => setConfirmAction('request')}
            >
              Become an organiser
            </button>
          )}
          {isPendingOrganiser && (
            <>
              <span className="status-pill">Organiser request pending</span>
              <button
                className="button button-secondary button-small"
                type="button"
                onClick={() => setConfirmAction('cancel')}
              >
                Cancel request
              </button>
            </>
          )}
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
        {outcomeBanner && (
          <div
            className={outcomeBanner === 'APPROVED' ? 'notice notice-success' : 'notice notice-error'}
            role="alert"
          >
            {outcomeBanner === 'APPROVED'
              ? 'Your organiser request has been accepted.'
              : 'Your organiser request has been denied.'}
            <button type="button" className="notice-dismiss" onClick={() => setOutcomeBanner(null)} aria-label="Dismiss">
              ×
            </button>
          </div>
        )}
        {children}
      </main>

      {confirmAction === 'request' && (
        <ConfirmDialog
          title="Request organiser access"
          message="This will send an organiser request to the platform admin for review. You'll be able to view organiser pages while it's pending, but you won't be able to create or publish anything until it's approved."
          confirmLabel="Send request"
          onConfirm={handleConfirmRequest}
          onClose={() => setConfirmAction(null)}
        />
      )}

      {confirmAction === 'cancel' && (
        <ConfirmDialog
          title="Cancel organiser request"
          message="Are you sure you want to cancel your pending organiser request? You'll go back to a regular customer account."
          confirmLabel="Cancel request"
          danger
          onConfirm={handleConfirmCancel}
          onClose={() => setConfirmAction(null)}
        />
      )}
    </div>
  );
}
