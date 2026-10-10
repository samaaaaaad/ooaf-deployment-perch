import { useEffect, useRef, useState } from 'react';

import {
  acknowledgeOrganiserRequest,
  cancelOrganiserRequest,
  getLatestOrganiserRequest,
  submitOrganiserRequest,
} from '../api/organiserRequestApi';
import {
  acknowledgeStudentVerification,
  cancelStudentVerification,
  getLatestStudentVerification,
} from '../api/studentVerificationApi';
import { useAuth } from '../auth/AuthContext';
import { Link, NavLink, useRouter } from '../utils/router';
import ConfirmDialog from './ConfirmDialog';
import StudentVerificationModal from './StudentVerificationModal';

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
  const [confirmAction, setConfirmAction] = useState(null); // 'request' | 'cancel' | 'cancel-student'
  const [latestStudentRequest, setLatestStudentRequest] = useState(null);
  const [studentOutcome, setStudentOutcome] = useState(null); // 'APPROVED' | 'REJECTED'
  const [showStudentModal, setShowStudentModal] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [pendingNoticeDismissed, setPendingNoticeDismissed] = useState(false);
  const menuRef = useRef(null);

  function handleLogout() {
    signOut();
    navigate('/login', { replace: true });
  }

  const isOrganiser = user?.role === 'ORGANISER';
  const isPendingOrganiser = user?.role === 'PENDING_ORGANISER';
  const isCustomer = user?.role === 'CUSTOMER';
  const isAdmin = user?.role === 'ADMIN';
  const canViewOrganiserPages = isOrganiser || isPendingOrganiser;
  const isVerifiedStudent = user?.studentEligible === true;
  const studentRequestPending = latestStudentRequest?.status === 'PENDING';

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

  // US 12: student verification state. An approval flips studentEligible on the stored user so
  // student prices show straight away; the server checks verification itself on every booking.
  useEffect(() => {
    if (!user || isAdmin) return;
    let active = true;
    getLatestStudentVerification(user.userId)
      .then((latest) => {
        if (!active || !latest) return;
        setLatestStudentRequest(latest);
        const verified = latest.status === 'APPROVED';
        if (verified !== isVerifiedStudent && (verified || latest.status === 'REJECTED')) {
          signIn({ ...user, studentEligible: verified });
        }
        if (latest.notified || (latest.status !== 'APPROVED' && latest.status !== 'REJECTED')) return;
        setStudentOutcome(latest.status);
        acknowledgeStudentVerification(user.userId, latest.requestId).catch(() => {});
      })
      .catch(() => {});
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.userId]);

  useEffect(() => {
    if (!menuOpen) return;
    function handleClickOutside(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [menuOpen]);

  async function handleConfirmRequest() {
    const updated = await submitOrganiserRequest(user.userId);
    signIn(updated);
    const latest = await getLatestOrganiserRequest(user.userId);
    setLatestRequest(latest);
    setPendingNoticeDismissed(false);
    setConfirmAction(null);
  }

  async function handleConfirmCancel() {
    const updated = await cancelOrganiserRequest(user.userId, latestRequest.requestId);
    signIn(updated);
    setLatestRequest(null);
    setConfirmAction(null);
  }

  async function handleConfirmCancelStudent() {
    const cancelled = await cancelStudentVerification(user.userId, latestStudentRequest.requestId);
    setLatestStudentRequest(cancelled);
    setConfirmAction(null);
  }

  function handleStudentSubmitted(created) {
    setLatestStudentRequest(created);
    setShowStudentModal(false);
    setStudentOutcome(null);
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
          {!isAdmin && <NavLink to="/tickets">My Tickets</NavLink>}
          {isAdmin && <NavLink to="/admin">Dashboard</NavLink>}
          {canViewOrganiserPages && <NavLink to="/events">My Events</NavLink>}
          {canViewOrganiserPages && <NavLink to="/floorplans">Floor Plans</NavLink>}
          {canViewOrganiserPages && <NavLink to="/venues">Venues</NavLink>}
        </nav>

        <div className="account-menu" ref={menuRef}>
          <button
            type="button"
            className="avatar-button"
            onClick={() => setMenuOpen((v) => !v)}
            aria-label="Account menu"
            aria-expanded={menuOpen}
          >
            <span className="avatar" aria-hidden="true">{initials(user.fullName)}</span>
          </button>

          {menuOpen && (
            <div className="account-popover">
              <div className="account-popover-header">
                <span className="avatar avatar-large" aria-hidden="true">{initials(user.fullName)}</span>
                <strong>{user.fullName}</strong>
                <small>{user.email}</small>
                {isVerifiedStudent && <small style={{ color: 'var(--brand)', fontWeight: 700 }}>🎓 Verified student</small>}
                {!isVerifiedStudent && studentRequestPending && <small>Student verification pending</small>}
              </div>

              {!isAdmin && !isVerifiedStudent && (
                <>
                  <div className="account-popover-divider" />
                  {studentRequestPending ? (
                    <button
                      type="button"
                      className="account-popover-item"
                      onClick={() => { setMenuOpen(false); setConfirmAction('cancel-student'); }}
                    >
                      Cancel student verification
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="account-popover-item"
                      onClick={() => { setMenuOpen(false); setShowStudentModal(true); }}
                    >
                      Verify student status
                    </button>
                  )}
                </>
              )}

              {isCustomer && (
                <>
                  <div className="account-popover-divider" />
                  <button
                    type="button"
                    className="account-popover-item"
                    onClick={() => { setMenuOpen(false); setConfirmAction('request'); }}
                  >
                    Become an Organiser
                  </button>
                </>
              )}

              {isPendingOrganiser && (
                <>
                  <div className="account-popover-divider" />
                  <button
                    type="button"
                    className="account-popover-item"
                    onClick={() => { setMenuOpen(false); setConfirmAction('cancel'); }}
                  >
                    Cancel organiser request
                  </button>
                </>
              )}

              <div className="account-popover-divider" />
              <button type="button" className="account-popover-item" onClick={handleLogout}>
                Log out
              </button>
            </div>
          )}
        </div>
      </header>

      <main className="page-container">
        {isPendingOrganiser && !pendingNoticeDismissed && (
          <div className="notice notice-info" role="status">
            <span>Your organiser request is pending admin review.</span>
            <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
              <button type="button" className="notice-action" onClick={() => setConfirmAction('cancel')}>
                Cancel request
              </button>
              <button
                type="button"
                className="notice-dismiss"
                onClick={() => setPendingNoticeDismissed(true)}
                aria-label="Dismiss"
              >
                ×
              </button>
            </div>
          </div>
        )}
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
        {studentOutcome && (
          <div
            className={studentOutcome === 'APPROVED' ? 'notice notice-success' : 'notice notice-error'}
            role="alert"
          >
            {studentOutcome === 'APPROVED'
              ? 'Your student status has been verified. Student prices and students-only events are now available to you.'
              : 'Your student verification was not approved. You can apply again from your account menu.'}
            <button type="button" className="notice-dismiss" onClick={() => setStudentOutcome(null)} aria-label="Dismiss">
              ×
            </button>
          </div>
        )}
        {children}
      </main>

      {showStudentModal && (
        <StudentVerificationModal
          user={user}
          onClose={() => setShowStudentModal(false)}
          onSubmitted={handleStudentSubmitted}
        />
      )}

      {confirmAction === 'cancel-student' && (
        <ConfirmDialog
          title="Cancel student verification"
          message="Are you sure you want to withdraw your student verification request? You can apply again later."
          confirmLabel="Withdraw request"
          danger
          onConfirm={handleConfirmCancelStudent}
          onClose={() => setConfirmAction(null)}
        />
      )}

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
