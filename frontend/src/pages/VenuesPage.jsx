import { useEffect, useState } from 'react';

import { getVenues } from '../api/venueApi';
import { useAuth } from '../auth/AuthContext';
import EmptyState from '../components/EmptyState';
import LoadingState from '../components/LoadingState';
import { formatDate } from '../utils/formatDate';

export default function VenuesPage() {
  const { user } = useAuth();
  const [venues, setVenues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getVenues(user.userId)
      .then((data) => {
        if (active) setVenues(data || []);
      })
      .catch((requestError) => {
        if (active) setError(requestError.message || 'Unable to load venues');
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
          <p className="eyebrow">Location management</p>
          <h1>Venues</h1>
          <p>Physical performance locations owned by the current account.</p>
        </div>
        <span className="status-pill">Read-only</span>
      </div>

      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {loading ? (
        <LoadingState label="Loading venues" />
      ) : venues.length === 0 ? (
        <div className="content-card">
          <EmptyState
            title="No venues available"
            description="The backend read API is connected. Venue creation will arrive with authenticated write endpoints."
          />
        </div>
      ) : (
        <div className="venue-grid">
          {venues.map((venue) => (
            <article className="venue-card" key={venue.venueId}>
              <span className="venue-marker" aria-hidden="true">V</span>
              <div>
                <h2>{venue.name}</h2>
                <p>{venue.address}</p>
                <p>{venue.city}, {venue.state}</p>
                <small>Added {formatDate(venue.createdAt)}</small>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
