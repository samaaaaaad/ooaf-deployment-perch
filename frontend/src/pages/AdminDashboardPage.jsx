import { useEffect, useState } from 'react';

import {
  approveOrganiserRequest,
  approveStudentVerification,
  getPendingOrganiserRequests,
  getPendingStudentVerifications,
  getStats,
  rejectOrganiserRequest,
  rejectStudentVerification,
} from '../api/adminApi';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';
import LoadingState from '../components/LoadingState';
import { formatDate } from '../utils/formatDate';

export default function AdminDashboardPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [requests, setRequests] = useState([]);
  const [requestsLoading, setRequestsLoading] = useState(true);
  const [requestsError, setRequestsError] = useState('');
  const [pendingDecision, setPendingDecision] = useState(null); // { request, action: 'approve' | 'reject' }

  // US 12: student verification review.
  const [studentRequests, setStudentRequests] = useState([]);
  const [studentLoading, setStudentLoading] = useState(true);
  const [studentError, setStudentError] = useState('');
  const [studentDecision, setStudentDecision] = useState(null); // { request, action: 'approve' | 'reject' }
  const [viewingCard, setViewingCard] = useState(null);

  useEffect(() => {
    let active = true;
    getStats()
      .then((data) => {
        if (active) setStats(data);
      })
      .catch((requestError) => {
        if (active) setError(requestError.message || 'Unable to load statistics');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    let active = true;
    getPendingOrganiserRequests()
      .then((data) => {
        if (active) setRequests(data || []);
      })
      .catch((requestError) => {
        if (active) setRequestsError(requestError.message || 'Unable to load organiser requests');
      })
      .finally(() => {
        if (active) setRequestsLoading(false);
      });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    let active = true;
    getPendingStudentVerifications()
      .then((data) => {
        if (active) setStudentRequests(data || []);
      })
      .catch((requestError) => {
        if (active) setStudentError(requestError.message || 'Unable to load student verifications');
      })
      .finally(() => {
        if (active) setStudentLoading(false);
      });
    return () => { active = false; };
  }, []);

  async function handleConfirmStudentDecision() {
    const { request: req, action } = studentDecision;
    setStudentError('');
    const decide = action === 'approve' ? approveStudentVerification : rejectStudentVerification;
    await decide(req.requestId);
    setStudentRequests((prev) => prev.filter((r) => r.requestId !== req.requestId));
    setStudentDecision(null);
  }

  async function handleConfirmDecision() {
    const { request: req, action } = pendingDecision;
    setRequestsError('');
    const decide = action === 'approve' ? approveOrganiserRequest : rejectOrganiserRequest;
    await decide(req.requestId);
    setRequests((prev) => prev.filter((r) => r.requestId !== req.requestId));
    setPendingDecision(null);
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Platform overview</p>
          <h1>Admin dashboard</h1>
        </div>
      </div>

      {error && <div className="notice notice-error" role="alert">{error}</div>}

      {loading ? (
        <LoadingState label="Loading statistics" />
      ) : (
        <>
          <div className="content-card">
            <p className="eyebrow">Total revenue</p>
            <h2>${Number(stats?.totalRevenue || 0).toFixed(2)}</h2>
          </div>

          <div className="content-card">
            <div className="card-heading">
              <div>
                <p className="eyebrow">Popularity</p>
                <h2>Popular events by tickets sold</h2>
              </div>
            </div>

            {!stats?.popularEventsByTicketsSold?.length ? (
              <EmptyState
                title="No tickets sold yet"
                description="Popular events will appear here once tickets start selling."
              />
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Event</th>
                      <th>Tickets sold</th>
                      <th>Revenue</th>
                    </tr>
                  </thead>
                  <tbody>
                    {stats.popularEventsByTicketsSold.map((event) => (
                      <tr key={event.eventId}>
                        <td><strong>{event.title}</strong></td>
                        <td>{event.ticketsSold}</td>
                        <td>${Number(event.revenue).toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}

      <div className="content-card">
        <div className="card-heading">
          <div>
            <p className="eyebrow">Organiser access</p>
            <h2>Pending organiser requests</h2>
          </div>
        </div>

        {requestsError && <div className="notice notice-error" role="alert">{requestsError}</div>}

        {requestsLoading ? (
          <LoadingState label="Loading requests" />
        ) : requests.length === 0 ? (
          <EmptyState
            title="No pending requests"
            description="Organiser applications will show up here for you to approve or reject."
          />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Applicant</th>
                  <th>Requested</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((req) => (
                  <tr key={req.requestId}>
                    <td>
                      <strong>{req.userFullName}</strong>
                      <small>{req.userEmail}</small>
                    </td>
                    <td>{formatDate(req.createdAt)}</td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                        <button
                          type="button"
                          className="button button-primary button-small"
                          onClick={() => setPendingDecision({ request: req, action: 'approve' })}
                        >
                          Accept
                        </button>
                        <button
                          type="button"
                          className="button button-secondary button-small"
                          onClick={() => setPendingDecision({ request: req, action: 'reject' })}
                        >
                          Reject
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="content-card">
        <div className="card-heading">
          <div>
            <p className="eyebrow">Student pricing</p>
            <h2>Pending student verifications</h2>
          </div>
        </div>

        {studentError && <div className="notice notice-error" role="alert">{studentError}</div>}

        {studentLoading ? (
          <LoadingState label="Loading student verifications" />
        ) : studentRequests.length === 0 ? (
          <EmptyState
            title="No pending verifications"
            description="Student verification requests will show up here. Check each student card before approving."
          />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Applicant</th>
                  <th>University</th>
                  <th>Student no.</th>
                  <th>Student card</th>
                  <th>Requested</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {studentRequests.map((req) => (
                  <tr key={req.requestId}>
                    <td>
                      <strong>{req.userFullName}</strong>
                      <small>{req.userEmail}</small>
                    </td>
                    <td>{req.university}</td>
                    <td>{req.studentNumber}</td>
                    <td>
                      {req.cardImage ? (
                        <button
                          type="button"
                          onClick={() => setViewingCard(req)}
                          aria-label={`View student card for ${req.userFullName}`}
                          style={{ background: 'none', border: 0, padding: 0, cursor: 'zoom-in' }}
                        >
                          <img
                            src={req.cardImage}
                            alt=""
                            style={{ width: '72px', height: '46px', objectFit: 'cover', borderRadius: '6px', border: '1px solid #cbd5d9' }}
                          />
                        </button>
                      ) : (
                        <small>No photo</small>
                      )}
                    </td>
                    <td>{formatDate(req.createdAt)}</td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                        <button
                          type="button"
                          className="button button-primary button-small"
                          onClick={() => setStudentDecision({ request: req, action: 'approve' })}
                        >
                          Approve
                        </button>
                        <button
                          type="button"
                          className="button button-secondary button-small"
                          onClick={() => setStudentDecision({ request: req, action: 'reject' })}
                        >
                          Reject
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {viewingCard && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={`Student card for ${viewingCard.userFullName}`}
          onClick={() => setViewingCard(null)}
          style={{
            position: 'fixed', inset: 0, backgroundColor: 'rgba(10, 35, 40, 0.85)', zIndex: 100,
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '1rem', gap: '0.75rem',
          }}
        >
          <img src={viewingCard.cardImage} alt={`Student card for ${viewingCard.userFullName}`} style={{ maxWidth: '100%', maxHeight: '75vh', borderRadius: '10px' }} />
          <p style={{ color: '#fff', margin: 0, textAlign: 'center' }}>
            {viewingCard.userFullName} · {viewingCard.university} · {viewingCard.studentNumber}
          </p>
          <button type="button" className="button button-secondary" onClick={() => setViewingCard(null)}>Close</button>
        </div>
      )}

      {studentDecision && (
        <ConfirmDialog
          title={studentDecision.action === 'approve' ? 'Approve student verification' : 'Reject student verification'}
          message={
            studentDecision.action === 'approve'
              ? `Approve ${studentDecision.request.userEmail} as a student at ${studentDecision.request.university}? They'll get student prices and access to students-only events. The card photo will be deleted.`
              : `Reject the student verification for ${studentDecision.request.userEmail}? The card photo will be deleted and they can apply again.`
          }
          confirmLabel={studentDecision.action === 'approve' ? 'Approve' : 'Reject'}
          danger={studentDecision.action === 'reject'}
          onConfirm={handleConfirmStudentDecision}
          onClose={() => setStudentDecision(null)}
        />
      )}

      {pendingDecision && (
        <ConfirmDialog
          title={pendingDecision.action === 'approve' ? 'Accept organiser request' : 'Reject organiser request'}
          message={
            pendingDecision.action === 'approve'
              ? `Are you sure you want ${pendingDecision.request.userEmail} to be accepted as an organiser?`
              : `Are you sure you want ${pendingDecision.request.userEmail} to be rejected?`
          }
          confirmLabel={pendingDecision.action === 'approve' ? 'Accept' : 'Reject'}
          danger={pendingDecision.action === 'reject'}
          onConfirm={handleConfirmDecision}
          onClose={() => setPendingDecision(null)}
        />
      )}
    </section>
  );
}
