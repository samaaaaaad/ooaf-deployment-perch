import { useEffect, useState } from 'react';

import { getEvents } from '../api/eventApi';
import { getVenues } from '../api/venueApi';
import { useAuth } from '../auth/AuthContext';
import EmptyState from '../components/EmptyState';
import LoadingState from '../components/LoadingState';
import { formatDate } from '../utils/formatDate';
import { Link } from '../utils/router';

export default function DashboardPage() {
  const { user } = useAuth();
  const [events, setEvents] = useState([]);
  const [venues, setVenues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    Promise.all([getEvents(user.userId), getVenues(user.userId)])
      .then(([nextEvents, nextVenues]) => {
        if (!active) return;
        setEvents(nextEvents || []);
        setVenues(nextVenues || []);
      })
      .catch((requestError) => {
        if (active) setError(requestError.message || 'Unable to load the dashboard');
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [user.userId]);

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Overview</p>
          <h1>Good to see you, {user.fullName.split(' ')[0]}.</h1>
          <p>Your Event and Venue data from the Sprint 1 backend appears here.</p>
        </div>
        <span className="status-pill">Read-only MVP</span>
      </div>

      {error && <div className="notice notice-error" role="alert">{error}</div>}

      {loading ? (
        <LoadingState label="Loading workspace" />
      ) : (
        <>
          <div className="metric-grid">
            <Link className="metric-card" to="/events">
              <span className="metric-label">Events</span>
              <strong className="metric-value">{events.length}</strong>
              <span>View productions and publication status →</span>
            </Link>
            <Link className="metric-card" to="/venues">
              <span className="metric-label">Venues</span>
              <strong className="metric-value">{venues.length}</strong>
              <span>View saved performance locations →</span>
            </Link>
          </div>

          <div className="content-card">
            <div className="card-heading">
              <div>
                <p className="eyebrow">Latest records</p>
                <h2>Recent events</h2>
              </div>
              <Link to="/events">View all</Link>
            </div>

            {events.length === 0 ? (
              <EmptyState
                title="No events yet"
                description="The Event repository is ready. Creation will be added after authenticated write endpoints are available."
              />
            ) : (
              <div className="record-list">
                {events.slice(0, 3).map((event) => (
                  <article className="record-row" key={event.eventId}>
                    <div>
                      <strong>{event.title}</strong>
                      <span>{event.description || 'No description supplied'}</span>
                    </div>
                    <div className="record-meta">
                      <span className={`status-badge status-${event.status?.toLowerCase()}`}>
                        {event.status}
                      </span>
                      <small>{formatDate(event.createdAt)}</small>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </section>
  );
}
