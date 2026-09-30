import { useEffect, useState } from 'react';

import {
  approveOrganiserRequest,
  getPendingOrganiserRequests,
  getStats,
  rejectOrganiserRequest,
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
