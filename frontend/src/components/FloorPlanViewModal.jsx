import { useEffect, useState } from 'react';
import { getFloorplanSeats } from '../api/eventApi';
import { CustomPreview, StagePreview } from './FloorPlanPreview';

// US 13.0 / 13.5: the enlarged "View" popup for a floor plan card — used by the
// shared gallery (and reusable for an organiser's own plans) to show the full
// layout, who it's published by, and an optional "Add to My Floor Plans" action.
export default function FloorPlanViewModal({ organiserId, plan, onClose, onAddToMine, adding }) {
    const [seats, setSeats] = useState(null);
    const [loading, setLoading] = useState(plan.layoutType !== 'STAGE');

    useEffect(() => {
        if (plan.layoutType === 'STAGE') {
            setSeats(null);
            return undefined;
        }
        let active = true;
        setLoading(true);
        getFloorplanSeats(organiserId, plan.floorPlanId)
            .then((data) => { if (active) setSeats(data || []); })
            .catch(() => { if (active) setSeats([]); })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, [organiserId, plan.floorPlanId, plan.layoutType]);

    return (
        <div
            style={{
                position: 'fixed', inset: 0, backgroundColor: 'rgba(10, 35, 40, 0.75)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '1rem',
                backdropFilter: 'blur(4px)',
            }}
            onClick={onClose}
        >
            <div
                style={{
                    background: '#ffffff', borderRadius: '16px', maxWidth: '760px', width: '100%',
                    padding: '2rem', boxShadow: '0 20px 40px rgba(0,0,0,0.2)', maxHeight: '90vh', overflowY: 'auto',
                }}
                onClick={(e) => e.stopPropagation()}
            >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                    <div>
                        <p className="eyebrow" style={{ margin: 0 }}>{plan.capacity} seats · {plan.layoutType}</p>
                        <h2 style={{ margin: '0.25rem 0 0', fontSize: '1.4rem' }}>{plan.name}</h2>
                        {plan.ownerName && (
                            <p style={{ margin: '0.25rem 0 0', color: 'var(--muted, #667681)', fontSize: '0.85rem' }}>
                                Published by {plan.ownerName}
                                {plan.sharedAt && ` on ${new Date(plan.sharedAt).toLocaleDateString()}`}
                            </p>
                        )}
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        aria-label="Close floor plan preview"
                        style={{ background: 'transparent', border: 0, fontSize: '1.5rem', cursor: 'pointer', color: '#667681' }}
                    >
                        ×
                    </button>
                </div>

                {loading ? (
                    <div style={{ background: '#0a2328', borderRadius: '12px', padding: '1.25rem', textAlign: 'center', color: '#c7d6d8', fontSize: '0.85rem' }}>
                        Loading floor plan preview…
                    </div>
                ) : seats ? (
                    <CustomPreview seats={seats} layoutJson={plan.layoutJson} maxHeight={420} />
                ) : (
                    <StagePreview maxHeight={420} />
                )}

                {onAddToMine && (
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                        <button type="button" className="button button-secondary" onClick={onClose} disabled={adding}>
                            Close
                        </button>
                        <button type="button" className="button button-primary" onClick={onAddToMine} disabled={adding}>
                            {adding ? 'Adding…' : 'Add to My Floor Plans'}
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}
