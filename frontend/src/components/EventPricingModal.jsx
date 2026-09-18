import { useEffect, useState } from 'react';
import { getFloorplans, updateEventPricing } from '../api/eventApi';
import { useRouter } from '../utils/router';

// Reusable SVG Stage Layout generator with Premium (rows A-B) and Standard (rows C-F) seats
function StagePreview({ standardPrice, premiumPrice }) {
  const rowLetters = 'ABCDEF';
  const seats = [];
  for (let r = 0; r < 6; r += 1) {
    const isPremium = r < 2; // Rows A & B are premium front rows
    for (let c = 0; c < 10; c += 1) {
      const aisle = c >= 5 ? 24 : 0;
      seats.push({
        id: `${rowLetters[r]}${c + 1}`,
        label: `${rowLetters[r]}${c + 1}`,
        isPremium,
        price: isPremium ? premiumPrice : standardPrice,
        x: 182 + c * 26 + aisle,
        y: 84 + r * 30,
      });
    }
  }

  return (
    <div style={{ background: '#0a2328', borderRadius: '12px', padding: '1.25rem', textAlign: 'center', marginTop: '1rem' }}>
      <div style={{ color: '#d8a84e', fontWeight: 700, fontSize: '0.85rem', marginBottom: '0.75rem' }}>
        Stage Layout Preview (60 Seats · Premium & Standard Tiers)
      </div>
      <svg viewBox="0 0 640 280" style={{ width: '100%', height: 'auto', maxHeight: '240px' }} role="img" aria-label="Stage floorplan preview">
        {/* Stage Area */}
        <rect x="200" y="16" width="240" height="34" rx="8" fill="#176b63" />
        <text x="320" y="38" textAnchor="middle" fill="#ffffff" fontSize="13" fontWeight="bold">STAGE</text>

        {/* 60 Seats */}
        {seats.map((s) => (
          <g key={s.id}>
            <circle cx={s.x} cy={s.y} r="8" fill={s.isPremium ? '#8b5cf6' : '#3b82f6'} />
            <title>{`Seat ${s.label} · ${s.isPremium ? 'Premium' : 'Standard'} · $${s.price || '0.00'}`}</title>
          </g>
        ))}
      </svg>
      <div style={{ display: 'flex', justifyContent: 'center', gap: '1.5rem', marginTop: '0.65rem', fontSize: '0.78rem', color: '#c7d6d8' }}>
        <span><i style={{ display: 'inline-block', width: 10, height: 10, borderRadius: '50%', background: '#8b5cf6', marginRight: 6 }} />Premium (Rows A-B): ${premiumPrice || '0.00'}</span>
        <span><i style={{ display: 'inline-block', width: 10, height: 10, borderRadius: '50%', background: '#3b82f6', marginRight: 6 }} />Standard (Rows C-F): ${standardPrice || '0.00'}</span>
      </div>
    </div>
  );
}

export default function EventPricingModal({ organiserId, event, onClose, onSaved }) {
  const { navigate } = useRouter();
  const [floorplans, setFloorplans] = useState([]);
  const [selectedPlanId, setSelectedPlanId] = useState(event.floorPlanId || '');
  const [standardPrice, setStandardPrice] = useState(
    event.standardPrice !== null && event.standardPrice !== undefined ? String(event.standardPrice) : '25.00'
  );
  const [premiumPrice, setPremiumPrice] = useState(
    event.premiumPrice !== null && event.premiumPrice !== undefined ? String(event.premiumPrice) : '35.00'
  );
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getFloorplans(organiserId)
      .then((data) => {
        if (!active) return;
        setFloorplans(data || []);
        if (!selectedPlanId && data && data.length > 0) {
          setSelectedPlanId(data[0].floorPlanId);
        }
      })
      .catch((err) => {
        if (active) setError(err.message || 'Unable to load floorplans');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [organiserId, selectedPlanId]);

  async function handleSave(e) {
    e.preventDefault();
    const numStandard = parseFloat(standardPrice);
    const numPremium = parseFloat(premiumPrice);
    if (isNaN(numStandard) || numStandard < 0) {
      setError('Please enter a valid non-negative standard ticket price');
      return;
    }
    if (isNaN(numPremium) || numPremium < 0) {
      setError('Please enter a valid non-negative premium ticket price');
      return;
    }

    setSaving(true);
    setError('');
    try {
      const updated = await updateEventPricing(organiserId, event.eventId, {
        floorPlanId: selectedPlanId || null,
        standardPrice: numStandard,
        premiumPrice: numPremium,
      });
      onSaved(updated);
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to save pricing configuration');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, backgroundColor: 'rgba(10, 35, 40, 0.75)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '1rem',
      backdropFilter: 'blur(4px)'
    }}>
      <div style={{
        background: '#ffffff', borderRadius: '16px', maxWidth: '600px', width: '100%',
        padding: '2rem', boxShadow: '0 20px 40px rgba(0,0,0,0.2)', maxHeight: '90vh', overflowY: 'auto'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.25rem' }}>
          <div>
            <p className="eyebrow" style={{ margin: 0 }}>Seat Pricing & Layout</p>
            <h2 style={{ margin: '0.25rem 0 0', fontSize: '1.4rem' }}>{event.title}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            style={{ background: 'transparent', border: 0, fontSize: '1.5rem', cursor: 'pointer', color: '#667681' }}
            aria-label="Close modal"
          >
            ×
          </button>
        </div>

        {error && <div className="notice notice-error" role="alert">{error}</div>}

        <form onSubmit={handleSave} noValidate>
          <div className="field">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
              <label htmlFor="floorplan-select">Reusable Floorplan</label>
              <button
                type="button"
                onClick={() => navigate('/floorplan')}
                style={{ background: 'none', border: 0, color: 'var(--brand, #176b63)', cursor: 'pointer', fontWeight: 600, fontSize: '0.85rem', padding: 0 }}
              >
                Edit floorplan →
              </button>
            </div>
            <select
              id="floorplan-select"
              value={selectedPlanId}
              onChange={(e) => setSelectedPlanId(e.target.value)}
              disabled={loading || saving}
              style={{
                width: '100%', minHeight: '46px', padding: '0.72rem 0.8rem',
                borderRadius: '10px', border: '1px solid #cbd5d9', background: '#fff'
              }}
            >
              {floorplans.map((fp) => (
                <option key={fp.floorPlanId} value={fp.floorPlanId}>
                  {fp.name} ({fp.capacity} seats)
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="field">
              <label htmlFor="standard-price">Standard Price ($ AUD)</label>
              <input
                id="standard-price"
                type="number"
                min="0"
                step="0.50"
                value={standardPrice}
                onChange={(e) => setStandardPrice(e.target.value)}
                placeholder="25.00"
                required
                disabled={saving}
              />
              <small>Rows C through F.</small>
            </div>

            <div className="field">
              <label htmlFor="premium-price">Premium Price ($ AUD)</label>
              <input
                id="premium-price"
                type="number"
                min="0"
                step="0.50"
                value={premiumPrice}
                onChange={(e) => setPremiumPrice(e.target.value)}
                placeholder="35.00"
                required
                disabled={saving}
              />
              <small>Front rows A and B.</small>
            </div>
          </div>

          {/* Responsive SVG Floorplan Preview */}
          <StagePreview standardPrice={standardPrice} premiumPrice={premiumPrice} />

          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
            <button
              type="button"
              className="button button-secondary"
              onClick={onClose}
              disabled={saving}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="button button-primary"
              disabled={saving || loading}
            >
              {saving ? 'Saving to DB…' : 'Save Configuration'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
