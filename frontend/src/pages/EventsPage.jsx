import { useEffect, useState } from 'react';

import { createEvent, deleteEvent, getEvents, publishEvent } from '../api/eventApi';
import { categories } from '../utils/eventDisplay';
import { useAuth } from '../auth/AuthContext';
import { useRouter } from '../utils/router';
import EmptyState from '../components/EmptyState';
import EventPricingModal from '../components/EventPricingModal';
import LoadingState from '../components/LoadingState';
import PublishEventModal from '../components/PublishEventModal';
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
  const [startsAt, setStartsAt] = useState('');
  const [category, setCategory] = useState('');
  const [venueName, setVenueName] = useState('');
  const [city, setCity] = useState('');
  const [universitySpecific, setUniversitySpecific] = useState(false);
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
        universitySpecific,
        // datetime-local gives a local wall-clock value; send it as a proper ISO instant.
        startsAt: startsAt ? new Date(startsAt).toISOString() : null,
        category: category || null,
        venueName: venueName.trim() || null,
        city: city.trim() || null,
      });
      onCreated(created);
      setTitle('');
      setDescription('');
      setStartsAt('');
      setCategory('');
      setVenueName('');
      setCity('');
      setUniversitySpecific(false);
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

          <div className="field">
            <label htmlFor="event-starts-at">Date and time</label>
            <input
                id="event-starts-at"
                name="startsAt"
                type="datetime-local"
                value={startsAt}
                onChange={(event) => setStartsAt(event.target.value)}
            />
            <small>Optional. Once this time passes, the event drops off the Browse page.</small>
          </div>

          <div className="field">
            <label htmlFor="event-category">Category</label>
            <select id="event-category" name="category" value={category} onChange={(event) => setCategory(event.target.value)}>
              <option value="">Choose a category (optional)</option>
              {categories.map((c) => <option key={c.key} value={c.key}>{c.label}</option>)}
            </select>
          </div>

          <div className="field">
            <label htmlFor="event-venue">Venue</label>
            <input
                id="event-venue"
                name="venueName"
                type="text"
                value={venueName}
                onChange={(event) => setVenueName(event.target.value)}
                placeholder="e.g. Basement Bar"
                maxLength={150}
            />
          </div>

          <div className="field">
            <label htmlFor="event-city">Suburb or city</label>
            <input
                id="event-city"
                name="city"
                type="text"
                value={city}
                onChange={(event) => setCity(event.target.value)}
                placeholder="e.g. Sydney CBD"
                maxLength={100}
            />
          </div>

          <div className="field">
            <label htmlFor="event-university" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
              <input
                  id="event-university"
                  type="checkbox"
                  checked={universitySpecific}
                  onChange={(event) => setUniversitySpecific(event.target.checked)}
                  style={{ width: 'auto', minHeight: 0 }}
              />
              University students only
            </label>
            <small>Only students verified by an admin can book. Students can find it with the University events filter.</small>
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
  const { navigate } = useRouter();
  const isOrganiser = user.role === 'ORGANISER';
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [publishingId, setPublishingId] = useState(null);
  const [deletingId, setDeletingId] = useState(null);
  const [publishModalEvent, setPublishModalEvent] = useState(null);
  const [editingPricingEvent, setEditingPricingEvent] = useState(null);

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

  function handlePricingSaved(updatedEvent) {
    setEvents((prev) =>
      prev.map((ev) => (ev.eventId === updatedEvent.eventId ? updatedEvent : ev))
    );
  }

  function openPublishModal(event) {
    if (!event.floorPlanId || event.standardPrice === null || event.standardPrice === undefined) {
      setError('Please configure the floor plan and ticket pricing before publishing.');
      return;
    }
    setError('');
    setPublishModalEvent(event);
  }

  async function handlePublish(imageUrl) {
    const event = publishModalEvent;
    setPublishingId(event.eventId);
    try {
      const published = await publishEvent(user.userId, event.eventId, imageUrl);
      setEvents((prev) =>
        prev.map((ev) => (ev.eventId === published.eventId ? published : ev))
      );
      setPublishModalEvent(null);
    } finally {
      setPublishingId(null);
    }
  }

  async function handleDelete(event) {
    if (!window.confirm(`Delete "${event.title}"? This cannot be undone.`)) return;
    setError('');
    setDeletingId(event.eventId);
    try {
      await deleteEvent(user.userId, event.eventId);
      setEvents((prev) => prev.filter((ev) => ev.eventId !== event.eventId));
    } catch (requestError) {
      setError(requestError.message || 'Unable to delete event');
    } finally {
      setDeletingId(null);
    }
  }

  return (
      <section>
        <div className="page-heading">
          <div>
            <p className="eyebrow">Event management</p>
            <h1>Events</h1>
            <p>Productions are independent from their future dated venue sessions.</p>
          </div>
          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
            {isOrganiser && (
              <button type="button" className="button button-secondary" onClick={() => navigate('/floorplans')}>
                Manage Floor Plans
              </button>
            )}
            <span className="status-pill">{isOrganiser ? 'Draft on create' : 'Read-only'}</span>
          </div>
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
                <tr>
                  <th>Event</th>
                  <th>Floor Plan</th>
                  <th>Tier Pricing</th>
                  <th>Status</th>
                  <th>Created</th>
                  {isOrganiser && <th>Actions</th>}
                </tr>
                </thead>
                <tbody>
                {events.map((event) => {
                    const isDraft = event.status === 'DRAFT';
                    const hasPricing = event.standardPrice !== null && event.standardPrice !== undefined;
                    return (
                    <tr key={event.eventId}>
                      <td>
                        <strong>{event.title}</strong>
                        {event.universitySpecific && <small style={{ color: 'var(--brand)', fontWeight: 700 }}>🎓 Students only</small>}
                        <small>{event.description || 'No description'}</small>
                      </td>
                      <td>
                        {event.floorPlanName ? (
                          <span style={{ fontWeight: 600, color: 'var(--brand)' }}>{event.floorPlanName}</span>
                        ) : (
                          <small style={{ color: 'var(--muted)' }}>No layout assigned</small>
                        )}
                      </td>
                      <td>
                        {hasPricing ? (
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', fontSize: '0.8rem' }}>
                            <span><strong>Std:</strong> ${Number(event.standardPrice).toFixed(2)}</span>
                            {event.premiumPrice && (
                              <span style={{ color: '#7c3aed' }}><strong>Prem:</strong> ${Number(event.premiumPrice).toFixed(2)}</span>
                            )}
                            {event.studentDiscountPercent > 0 && (
                              <span style={{ color: 'var(--brand)' }}><strong>Student:</strong> −{event.studentDiscountPercent}%</span>
                            )}
                          </div>
                        ) : (
                          <small style={{ color: 'var(--muted)' }}>Unpriced</small>
                        )}
                      </td>
                      <td><span className={`status-badge status-${event.status?.toLowerCase()}`}>{event.status}</span></td>
                      <td>{formatDate(event.createdAt)}</td>
                      {isOrganiser && (
                        <td>
                          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                            <button
                              type="button"
                              className="button button-secondary button-small"
                              onClick={() => setEditingPricingEvent(event)}
                            >
                              Configure Pricing
                            </button>
                            {isDraft && (
                              <button
                                type="button"
                                className="button button-primary button-small"
                                onClick={() => openPublishModal(event)}
                                disabled={publishingId === event.eventId || !hasPricing}
                                title={!hasPricing ? 'Configure pricing before publishing' : 'Publish event to Browse'}
                              >
                                {publishingId === event.eventId ? 'Publishing…' : 'Publish'}
                              </button>
                            )}
                            <button
                              type="button"
                              className="button button-secondary button-small"
                              onClick={() => handleDelete(event)}
                              disabled={deletingId === event.eventId}
                            >
                              {deletingId === event.eventId ? 'Deleting…' : 'Delete'}
                            </button>
                          </div>
                        </td>
                      )}
                    </tr>
                    );
                })}
                </tbody>
              </table>
            </div>
        )}

        {editingPricingEvent && (
          <EventPricingModal
            organiserId={user.userId}
            event={editingPricingEvent}
            onClose={() => setEditingPricingEvent(null)}
            onSaved={handlePricingSaved}
          />
        )}

        {publishModalEvent && (
          <PublishEventModal
            event={publishModalEvent}
            onClose={() => setPublishModalEvent(null)}
            onPublish={handlePublish}
          />
        )}
      </section>
  );
}