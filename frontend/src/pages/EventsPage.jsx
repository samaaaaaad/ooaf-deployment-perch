import { useEffect, useState } from 'react';

import { createEvent, getEvents } from '../api/eventApi';
import { useAuth } from '../auth/AuthContext';
import EmptyState from '../components/EmptyState';
import LoadingState from '../components/LoadingState';
import { formatDate } from '../utils/formatDate';

const MAX_TITLE_LENGTH = 150;

// Mirrors EventService's server-side checks so the person gets instant
// feedback; the backend remains the source of truth and is re-checked on submit.
function validateTitle(title) {
  const trimmed = title.trim();
  if (!trimmed) return 'Event title is required';
  if (trimmed.length > MAX_TITLE_LENGTH) return `Event title must be ${MAX_TITLE_LENGTH} characters or fewer`;
  return null;
}

function CreateEventForm({ organiserId, onCreated }) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();

    const titleError = validateTitle(title);
    if (titleError) {
      setFormError(titleError);
      return;
    }

    setFormError('');
    setSubmitting(true);
    try {
      const created = await createEvent(organiserId, {
        title: title.trim(),
        description: description.trim() || null,
      });
      onCreated(created);
      setTitle('');
      setDescription('');
    } catch (requestError) {
      setFormError(requestError.message || 'Unable to create event');
    } finally {
      setSubmitting(false);
    }
  }

  return (
      <div className="content-card">
        <div className="card-heading">
          <div>
            <p className="eyebrow">New production</p>
            <h2>Create an event</h2>
          </div>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="event-title">Title</label>
            <input
                id="event-title"
                name="title"
                type="text"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="e.g. Sunset Rooftop Sessions"
                maxLength={MAX_TITLE_LENGTH}
            />
          </div>

          <div className="field">
            <label htmlFor="event-description">Description</label>
            <textarea
                id="event-description"
                name="description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Optional details customers will see once this is published"
            />
          </div>

          {formError && <div className="form-message form-message-error" role="alert">{formError}</div>}

          <button className="button button-primary" type="submit" disabled={submitting}>
            {submitting ? 'Creating…' : 'Create event'}
          </button>
        </form>
      </div>
  );
}

export default function EventsPage() {
  const { user } = useAuth();
  const isOrganiser = user.role === 'ORGANISER';
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

  function handleCreated(created) {
    setEvents((prev) => [created, ...prev]);
  }

  return (
      <section>
        <div className="page-heading">
          <div>
            <p className="eyebrow">Event management</p>
            <h1>Events</h1>
            <p>Productions are independent from their future dated venue sessions.</p>
          </div>
          <span className="status-pill">{isOrganiser ? 'Draft on create' : 'Read-only'}</span>
        </div>

        {error && <div className="notice notice-error" role="alert">{error}</div>}

        {isOrganiser && (
            <div className="events-form-slot">
              <CreateEventForm organiserId={user.userId} onCreated={handleCreated} />
            </div>
        )}

        {loading ? (
            <LoadingState label="Loading events" />
        ) : events.length === 0 ? (
            <div className="content-card">
              <EmptyState
                  title="No events yet"
                  description={
                    isOrganiser
                        ? 'Create your first event above to get started. New events are saved as drafts.'
                        : 'This account has no events yet.'
                  }
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