import { useEffect, useState } from 'react';

import { cloneFloorplan, deleteFloorplan, getFloorplans, getSharedFloorplans } from '../api/eventApi';
import { useAuth } from '../auth/AuthContext';
import EmptyState from '../components/EmptyState';
import FloorPlanViewModal from '../components/FloorPlanViewModal';
import LoadingState from '../components/LoadingState';
import { useRouter } from '../utils/router';
import './FloorplansPage.css';

// Saved Floorplans hub. Lists the organiser's reusable plans so they can be
// edited in the builder and picked when configuring an event, plus (US 13.0)
// the shared gallery of plans every organiser has published, kept in its own
// section so it's never confused with the viewer's own private plans.
export default function FloorplansPage() {
  const { user } = useAuth();
  const { navigate } = useRouter();
  const canManage = user.role === 'ORGANISER';
  const [plans, setPlans] = useState([]);
  const [sharedPlans, setSharedPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deletingId, setDeletingId] = useState(null);
  const [viewingPlan, setViewingPlan] = useState(null);
  const [cloningId, setCloningId] = useState(null);
  const [note, setNote] = useState('');
  const [noteType, setNoteType] = useState('info');

  useEffect(() => {
    let active = true;
    getFloorplans(user.userId)
        .then((data) => { if (active) setPlans(data || []); })
        .catch((err) => { if (active) setError(err.message || 'Unable to load floor plans'); })
        .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user.userId]);

  // US 13.0 / 13.5: the shared gallery, visible to every organiser. Own published
  // plans are filtered out here so they aren't listed twice (they already show
  // under "Your floor plans" with a Published badge).
  useEffect(() => {
    let active = true;
    getSharedFloorplans()
        .then((data) => { if (active) setSharedPlans((data || []).filter((p) => p.organiserId !== user.userId)); })
        .catch(() => { if (active) setSharedPlans([]); });
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

  async function handleAddToMine(plan) {
    setCloningId(plan.floorPlanId);
    setNote('');
    try {
      const copy = await cloneFloorplan(user.userId, plan.floorPlanId);
      setPlans((prev) => [copy, ...prev]);
      setNoteType('info');
      setNote(`"${plan.name}" was added to your floor plans as "${copy.name}". You can edit it like any plan of your own.`);
      setViewingPlan(null);
    } catch (err) {
      setNoteType('warning');
      setNote(err.message || 'Unable to add this floor plan');
    } finally {
      setCloningId(null);
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
        {note && (
            <div className={noteType === 'warning' ? 'notice notice-error' : 'notice notice-info'} role={noteType === 'warning' ? 'alert' : 'status'}>
              {note}
            </div>
        )}

        {loading ? (
            <LoadingState label="Loading floor plans" />
        ) : owned.length === 0 ? (
            <div className="content-card">
              <EmptyState
                  title="No floor plans yet"
                  description="Create your first floor plan to start placing seats and a stage."
              />
            </div>
        ) : (
            <div className="fp-list">
              {owned.map((p) => (
                  <div key={p.floorPlanId} className="fp-list-card">
                    <div>
                      <strong>{p.name}</strong>
                      <small>
                        {p.capacity} seats · {p.layoutType}
                        {p.shared && <span className="status-badge status-published fp-inline-badge">Published</span>}
                      </small>
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

        {!loading && templates.length > 0 && (
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

        {/* US 13.0 / 13.4-13.6: Community Made — every organiser's published floor
          plans, nested right under the starter templates so it reads as another
          source of ready-made layouts, but kept visually distinct from the
          viewer's own private ones (AC #2). Each card is credited to the
          organiser who made it and when it was published (AC #4); clicking it
          opens a full-size preview, and "Add to My Floor Plans" copies it into
          the viewer's own editable list (AC #3). Shown regardless of whether the
          viewer has any plans of their own, so it never depends on that state. */}
        {!loading && (
            <>
              <h2 style={{ marginTop: '1.75rem', fontSize: '1.05rem' }}>Community Made</h2>
              <p className="fp-hint" style={{ marginBottom: '0.9rem' }}>
                Published by other organisers. Preview a layout, or add a copy to your own floor plans.
              </p>
              {sharedPlans.length === 0 ? (
                  <div className="content-card">
                    <EmptyState
                        title="Nothing published yet"
                        description="When an organiser publishes a floor plan from the builder, it shows up here for everyone to reuse."
                    />
                  </div>
              ) : (
                  <div className="fp-list">
                    {sharedPlans.map((p) => (
                        <div
                            key={p.floorPlanId}
                            className="fp-list-card fp-shared-card"
                            style={{ cursor: 'pointer' }}
                            onClick={() => setViewingPlan(p)}
                        >
                          <div>
                            <strong>{p.name}</strong>
                            <small>
                              {p.capacity} seats · {p.layoutType}
                              <br />
                              by {p.ownerName}{p.sharedAt && ` · ${new Date(p.sharedAt).toLocaleDateString()}`}
                            </small>
                          </div>
                          <div style={{ display: 'flex', gap: '0.5rem' }} onClick={(e) => e.stopPropagation()}>
                            <button
                                type="button"
                                className="button button-secondary button-small"
                                onClick={() => setViewingPlan(p)}
                            >
                              Preview
                            </button>
                            {canManage && (
                                <button
                                    type="button"
                                    className="button button-primary button-small"
                                    onClick={() => handleAddToMine(p)}
                                    disabled={cloningId === p.floorPlanId}
                                >
                                  {cloningId === p.floorPlanId ? 'Adding…' : 'Add'}
                                </button>
                            )}
                          </div>
                        </div>
                    ))}
                  </div>
              )}
            </>
        )}

        {viewingPlan && (
            <FloorPlanViewModal
                organiserId={user.userId}
                plan={viewingPlan}
                onClose={() => setViewingPlan(null)}
                onAddToMine={canManage ? () => handleAddToMine(viewingPlan) : undefined}
                adding={cloningId === viewingPlan.floorPlanId}
            />
        )}
      </section>
  );
}
