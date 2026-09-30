import { useEffect, useState } from 'react';

import {
  approveOrganiserRequest,
  getPendingOrganiserRequests,
  getStats,
  rejectOrganiserRequest,
} from '../api/adminApi';
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
  const [decidingId, setDecidingId] = useState(null);

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

  async function handleDecide(requestId, decide) {
    setRequestsError('');
    setDecidingId(requestId);
    try {
      await decide(requestId);
      setRequests((prev) => prev.filter((r) => r.requestId !== requestId));
    } catch (requestError) {
      setRequestsError(requestError.message || 'Unable to update this request');
    } finally {
      setDecidingId(null);
    }
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

          <div className="content-card table-wrap">
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
            )}
          </div>
        </>
      )}

      <div className="content-card table-wrap">
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
                        onClick={() => handleDecide(req.requestId, approveOrganiserRequest)}
                        disabled={decidingId === req.requestId}
                      >
                        {decidingId === req.requestId ? 'Working…' : 'Accept'}
                      </button>
                      <button
                        type="button"
                        className="button button-secondary button-small"
                        onClick={() => handleDecide(req.requestId, rejectOrganiserRequest)}
                        disabled={decidingId === req.requestId}
                      >
                        {decidingId === req.requestId ? 'Working…' : 'Reject'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}
