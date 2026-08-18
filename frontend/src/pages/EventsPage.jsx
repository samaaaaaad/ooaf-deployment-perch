import { useEffect, useState } from 'react';

import { getEvents } from '../api/eventApi';
import { useAuth } from '../auth/AuthContext';
import EmptyState from '../components/EmptyState';
import LoadingState from '../components/LoadingState';
import { formatDate } from '../utils/formatDate';

export default function EventsPage() {
  const { user } = useAuth();
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getEvents(user.userId)
      .then((data) => {
        if (active) setEvents(data || []);
      })
      .catch((requestError) => {
        if (active) setError(requestError.message || 'Unable to load events');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [user.userId]);

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Event management</p>
          <h1>Events</h1>
          <p>Productions are independent from their future dated venue sessions.</p>
        </div>
        <span className="status-pill">Read-only</span>
      </div>

      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {loading ? (
        <LoadingState label="Loading events" />
      ) : events.length === 0 ? (
        <div className="content-card">
          <EmptyState
            title="No events available"
            description="The backend read API is connected. Event creation will arrive with authenticated write endpoints."
          />
        </div>
      ) : (
        <div className="content-card table-wrap">
          <table>
            <thead>
              <tr><th>Event</th><th>Status</th><th>Created</th></tr>
            </thead>
            <tbody>
              {events.map((event) => (
                <tr key={event.eventId}>
                  <td><strong>{event.title}</strong><small>{event.description || 'No description'}</small></td>
                  <td><span className={`status-badge status-${event.status?.toLowerCase()}`}>{event.status}</span></td>
                  <td>{formatDate(event.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
