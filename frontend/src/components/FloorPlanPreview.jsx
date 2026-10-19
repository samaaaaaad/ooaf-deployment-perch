import { decorFromLayoutJson } from '../utils/floorPlanDecor';

// Reusable SVG Stage Layout generator with Premium (rows A-B) and Standard (rows C-F) seats.
// `maxHeight` lets callers render a small inline preview (EventPricingModal) or a bigger
// one (the shared floor plan gallery's "View" modal) from the same markup.
export function StagePreview({ standardPrice, premiumPrice, maxHeight = 240 }) {
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
            <svg viewBox="0 0 640 280" style={{ width: '100%', height: 'auto', maxHeight: `${maxHeight}px` }} role="img" aria-label="Stage floor plan preview">
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

// Preview for a custom (builder-drawn) floor plan: the same seats and decor (tables,
// stage, barriers), in the same positions, that publishing will actually persist as
// EventSeat rows / that the builder drew.
export function CustomPreview({ seats, layoutJson, standardPrice, premiumPrice, maxHeight = 240 }) {
    const decor = decorFromLayoutJson(layoutJson);
    if (seats.length === 0 && decor.length === 0) {
        return (
            <div style={{ background: '#0a2328', borderRadius: '12px', padding: '1.25rem', textAlign: 'center', marginTop: '1rem', color: '#c7d6d8', fontSize: '0.85rem' }}>
                This layout has no seats yet. Add seats to it in the Floor Plan builder.
            </div>
        );
    }
    return (
        <div style={{ background: '#0a2328', borderRadius: '12px', padding: '1.25rem', textAlign: 'center', marginTop: '1rem' }}>
            <div style={{ color: '#d8a84e', fontWeight: 700, fontSize: '0.85rem', marginBottom: '0.75rem' }}>
                Floor Plan Preview ({seats.length} seats)
            </div>
            <svg viewBox="0 0 900 560" style={{ width: '100%', height: 'auto', maxHeight: `${maxHeight}px` }} role="img" aria-label="Floor plan preview">
                {decor.map((d, i) => (d.shape === 'rect' ? (
                    <g key={`d${i}`}>
                        <rect x={d.x} y={d.y} width={d.w} height={d.h} rx="9" fill={d.kind === 'stage' ? '#176b63' : '#4b5563'} />
                        {d.kind === 'stage' && (
                            <text x={d.x + d.w / 2} y={d.y + d.h / 2 + 5} textAnchor="middle" fill="#ffffff" fontSize="14" fontWeight="bold">STAGE</text>
                        )}
                    </g>
                ) : (
                    <ellipse key={`d${i}`} cx={d.cx} cy={d.cy} rx={d.rx} ry={d.ry} fill="#14544d" />
                )))}
                {seats.map((s) => {
                    const isPremium = s.tier === 'PREMIUM';
                    const price = isPremium ? premiumPrice : standardPrice;
                    return (
                        <g key={s.id}>
                            <circle cx={s.x} cy={s.y} r="13" fill={isPremium ? '#8b5cf6' : '#3b82f6'} />
                            <title>{`Seat ${s.label} · ${isPremium ? 'Premium' : 'Standard'} · $${price || '0.00'}`}</title>
                        </g>
                    );
                })}
            </svg>
            <div style={{ display: 'flex', justifyContent: 'center', gap: '1.5rem', marginTop: '0.65rem', fontSize: '0.78rem', color: '#c7d6d8' }}>
                <span><i style={{ display: 'inline-block', width: 10, height: 10, borderRadius: '50%', background: '#8b5cf6', marginRight: 6 }} />Premium: ${premiumPrice || '0.00'}</span>
                <span><i style={{ display: 'inline-block', width: 10, height: 10, borderRadius: '50%', background: '#3b82f6', marginRight: 6 }} />Standard: ${standardPrice || '0.00'}</span>
            </div>
        </div>
    );
}
