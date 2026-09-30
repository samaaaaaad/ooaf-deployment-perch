import { useEffect, useState } from 'react';

import { deleteFloorplan, getFloorplans } from '../api/eventApi';
import { useAuth } from '../auth/AuthContext';
import EmptyState from '../components/EmptyState';
import LoadingState from '../components/LoadingState';
import { useRouter } from '../utils/router';
import './FloorplansPage.css';

// Saved Floorplans hub. Lists the organiser's reusable plans so they can be
// edited in the builder and picked when configuring an event.
export default function FloorplansPage() {
  const { user } = useAuth();
  const { navigate } = useRouter();
  const canManage = user.role === 'ORGANISER';
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deletingId, setDeletingId] = useState(null);

  useEffect(() => {
    let active = true;
    getFloorplans(user.userId)
      .then((data) => { if (active) setPlans(data || []); })
      .catch((err) => { if (active) setError(err.message || 'Unable to load floor plans'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user.userId]);

  async function handleDelete(plan) {
    if (!window.confirm(`Delete "${plan.name}"? This cannot be undone.`)) return;
    setError('');
    setDeletingId(plan.floorPlanId);
    try {
      await deleteFloorplan(user.userId, plan.floorPlanId);
      setPlans((prev) => prev.filter((p) => p.floorPlanId !== plan.floorPlanId));
    } catch (err) {
      setError(err.message || 'Unable to delete floor plan');
    } finally {
      setDeletingId(null);
    }
  }

  const owned = plans.filter((p) => p.organiserId === user.userId);
  const templates = plans.filter((p) => p.organiserId !== user.userId);

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Your floor plans</p>
          <h1>Saved Floor Plans</h1>
          <p>Design a seating layout once, then reuse it across your events.</p>
        </div>
        {canManage && (
          <button type="button" className="button button-primary" onClick={() => navigate('/floorplan')}>
            + New Floor Plan
          </button>
        )}
      </div>

      {error && <div className="notice notice-error" role="alert">{error}</div>}

      {loading ? (
        <LoadingState label="Loading floor plans" />
      ) : owned.length === 0 && templates.length === 0 ? (
        <div className="content-card">
          <EmptyState
            title="No floor plans yet"
            description="Create your first floor plan to start placing seats and a stage."
          />
        </div>
      ) : (
        <>
          {owned.length > 0 && (
            <div className="fp-list">
              {owned.map((p) => (
                <div key={p.floorPlanId} className="fp-list-card">
                  <div>
                    <strong>{p.name}</strong>
                    <small>{p.capacity} seats · {p.layoutType}</small>
                  </div>
                  {canManage ? (
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <button
                        type="button"
                        className="button button-secondary button-small"
                        onClick={() => navigate(`/floorplan?id=${p.floorPlanId}`)}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        className="button button-secondary button-small"
                        onClick={() => handleDelete(p)}
                        disabled={deletingId === p.floorPlanId}
                      >
                        {deletingId === p.floorPlanId ? 'Deleting…' : 'Delete'}
                      </button>
                    </div>
                  ) : (
                    <span className="status-pill">Read-only</span>
                  )}
                </div>
              ))}
            </div>
          )}

          {templates.length > 0 && (
            <>
              <h2 style={{ marginTop: '1.75rem', fontSize: '1.05rem' }}>Starter templates</h2>
              <div className="fp-list">
                {templates.map((p) => (
                  <div key={p.floorPlanId} className="fp-list-card is-template">
                    <div>
                      <strong>{p.name}</strong>
                      <small>{p.capacity} seats · template</small>
                    </div>
                    <span className="status-pill">Read-only</span>
                  </div>
                ))}
              </div>
            </>
          )}
        </>
      )}
    </section>
  );
}
