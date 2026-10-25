import { useEffect, useState } from 'react';

import { createVenue, deleteVenue, getVenues, updateVenue } from '../api/venueApi';
import { useAuth } from '../auth/AuthContext';
import EmptyState from '../components/EmptyState';
import LoadingState from '../components/LoadingState';
import { formatDate } from '../utils/formatDate';

const EMPTY_FORM = { name: '', address: '', city: '', state: '' };
const FIELDS = [
  { key: 'name', label: 'Venue name', placeholder: 'e.g. Basement Bar', maxLength: 120 },
  { key: 'address', label: 'Street address', placeholder: 'e.g. 7 Macquarie Place', maxLength: 255 },
  { key: 'city', label: 'Suburb or city', placeholder: 'e.g. Sydney', maxLength: 120 },
  { key: 'state', label: 'State', placeholder: 'e.g. NSW', maxLength: 80 },
];

// Mirrors VenueRequest's server-side checks so the organiser gets instant feedback.
function validate(form) {
  const missing = FIELDS.find((field) => !form[field.key].trim());
  return missing ? `${missing.label} is required` : '';
}

function VenueForm({ organiserId, editing, onSaved, onCancel }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setForm(editing
      ? { name: editing.name, address: editing.address, city: editing.city, state: editing.state }
      : EMPTY_FORM);
    setFormError('');
  }, [editing]);

  async function handleSubmit(event) {
    event.preventDefault();
    const error = validate(form);
    if (error) {
      setFormError(error);
      return;
    }
    const body = Object.fromEntries(Object.entries(form).map(([key, value]) => [key, value.trim()]));
    setFormError('');
    setSaving(true);
    try {
      const saved = editing
        ? await updateVenue(organiserId, editing.venueId, body)
        : await createVenue(organiserId, body);
      onSaved(saved, Boolean(editing));
      if (!editing) setForm(EMPTY_FORM);
    } catch (requestError) {
      setFormError(requestError.message || 'Unable to save the venue');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="content-card">
      <div className="card-heading">
        <div>
          <p className="eyebrow">{editing ? 'Edit venue' : 'New venue'}</p>
          <h2>{editing ? `Editing ${editing.name}` : 'Add a venue'}</h2>
        </div>
      </div>
      <form onSubmit={handleSubmit} noValidate>
        {FIELDS.map((field) => (
          <div className="field" key={field.key}>
            <label htmlFor={`venue-${field.key}`}>{field.label}</label>
            <input
              id={`venue-${field.key}`}
              type="text"
              value={form[field.key]}
              placeholder={field.placeholder}
              maxLength={field.maxLength}
              onChange={(event) => setForm((prev) => ({ ...prev, [field.key]: event.target.value }))}
            />
          </div>
        ))}
        {formError && <div className="form-message form-message-error" role="alert">{formError}</div>}
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button className="button button-primary" type="submit" disabled={saving}>
            {saving ? 'Saving…' : editing ? 'Save changes' : 'Add venue'}
          </button>
          {editing && (
            <button className="button button-secondary" type="button" onClick={onCancel}>Cancel</button>
          )}
        </div>
      </form>
    </div>
  );
}

export default function VenuesPage() {
  const { user } = useAuth();
  const canManage = user.role === 'ORGANISER';
  const [venues, setVenues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [editing, setEditing] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

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

  function handleSaved(saved, wasEdit) {
    setVenues((prev) => (wasEdit
      ? prev.map((venue) => (venue.venueId === saved.venueId ? saved : venue))
      : [saved, ...prev]));
    setEditing(null);
    setNotice(wasEdit ? `${saved.name} was updated.` : `${saved.name} was added.`);
  }

  async function handleDelete(venue) {
    if (!window.confirm(`Delete "${venue.name}"? Events already using it keep their location.`)) return;
    setDeletingId(venue.venueId);
    setError('');
    try {
      await deleteVenue(user.userId, venue.venueId);
      setVenues((prev) => prev.filter((v) => v.venueId !== venue.venueId));
      if (editing?.venueId === venue.venueId) setEditing(null);
      setNotice(`${venue.name} was deleted.`);
    } catch (requestError) {
      setError(requestError.message || 'Unable to delete the venue');
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Location management</p>
          <h1>Venues</h1>
          <p>Save the pop-up locations you use, then pick one when you create an event.</p>
        </div>
        {!canManage && <span className="status-pill">Read-only while your organiser request is reviewed</span>}
      </div>

      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {notice && <div className="notice notice-success" role="status">{notice}</div>}

      {canManage && (
        <VenueForm
          organiserId={user.userId}
          editing={editing}
          onSaved={handleSaved}
          onCancel={() => setEditing(null)}
        />
      )}

      {loading ? (
        <LoadingState label="Loading venues" />
      ) : venues.length === 0 ? (
        <div className="content-card">
          <EmptyState
            title="No venues yet"
            description={canManage
              ? 'Add your first venue above. You can then choose it when creating an event.'
              : 'Venues you save will appear here once your organiser access is approved.'}
          />
        </div>
      ) : (
        <div className="venue-grid" style={{ marginTop: '1.25rem' }}>
          {venues.map((venue) => (
            <article className="venue-card" key={venue.venueId}>
              <span className="venue-marker" aria-hidden="true">V</span>
              <div style={{ flex: 1 }}>
                <h2>{venue.name}</h2>
                <p>{venue.address}</p>
                <p>{venue.city}, {venue.state}</p>
                <small>Added {formatDate(venue.createdAt)}</small>
                {canManage && (
                  <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem' }}>
                    <button type="button" className="button button-secondary button-small" onClick={() => { setNotice(''); setEditing(venue); }}>
                      Edit
                    </button>
                    <button
                      type="button"
                      className="button button-secondary button-small"
                      onClick={() => handleDelete(venue)}
                      disabled={deletingId === venue.venueId}
                    >
                      {deletingId === venue.venueId ? 'Deleting…' : 'Delete'}
                    </button>
                  </div>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
