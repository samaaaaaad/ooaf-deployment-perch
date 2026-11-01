import { useCallback, useEffect, useRef, useState } from 'react';

import { createFloorplan, getFloorplans, publishFloorplan, unpublishFloorplan, updateFloorplan } from '../api/eventApi';
import { useAuth } from '../auth/AuthContext';
import { useRouter } from '../utils/router';
import './FloorPlanEditorPage.css';

// Floorplan builder (US 4.0). Organisers drag shapes from the palette onto the
// board, reposition and delete them, then save the plan to the backend so it
// becomes selectable when configuring an event.

const BOARD_W = 900;
const BOARD_H = 560;

// Per-shape geometry. Circles (seat, table) are positioned by their centre;
// rectangles (stage, barrier) by their top-left corner.
const SHAPES = {
  seat: { kind: 'circle', r: 13, label: 'Seat', counts: true },
  table: { kind: 'circle', r: 26, fill: '#14544d', label: 'Table', counts: false },
  stage: { kind: 'rect', w: 320, h: 64, fill: '#176b63', label: 'Stage', single: true },
  barrier: { kind: 'rect', w: 150, h: 16, fill: '#4b5563', label: 'Barrier', single: false },
};

// Seats carry a price tier (matches the backend's SeatTier enum); every other
// shape is untiered. Colors match FloorPlanPreview.jsx (the pricing modal and
// shared-gallery preview), so a seat looks the same tier here as it does there.
const SEAT_TIER_COLORS = { STANDARD: '#3b82f6', PREMIUM: '#8b5cf6' };
const seatTierOf = (item) => (item.tier === 'PREMIUM' ? 'PREMIUM' : 'STANDARD');

// Draggable palette tools. Standard Seat and Premium Seat are two distinct
// tools (not one "Seat" tool plus a toggle) so the tier is picked up front,
// the same way Table/Stage/Barrier are already picked by dragging the right tile.
const PALETTE_TOOLS = [
  { key: 'seat-standard', itemType: 'seat', tier: 'STANDARD', label: 'Standard Seat', fill: SEAT_TIER_COLORS.STANDARD },
  { key: 'seat-premium', itemType: 'seat', tier: 'PREMIUM', label: 'Premium Seat', fill: SEAT_TIER_COLORS.PREMIUM },
  { key: 'table', itemType: 'table', label: 'Table', fill: SHAPES.table.fill },
  { key: 'stage', itemType: 'stage', label: 'Stage', fill: SHAPES.stage.fill },
  { key: 'barrier', itemType: 'barrier', label: 'Barrier', fill: SHAPES.barrier.fill },
];

let idCounter = 0;
const nextId = () => {
  idCounter += 1;
  return `it-${Date.now()}-${idCounter}`;
};

const clamp = (v, min, max) => Math.min(Math.max(v, min), max);

function clampItem(type, x, y) {
  const s = SHAPES[type];
  if (s.kind === 'rect') {
    return { x: clamp(x, 0, BOARD_W - s.w), y: clamp(y, 0, BOARD_H - s.h) };
  }
  return { x: clamp(x, s.r, BOARD_W - s.r), y: clamp(y, s.r, BOARD_H - s.r) };
}

export default function FloorPlanEditorPage() {
  const { user } = useAuth();
  const { navigate } = useRouter();
  const canManage = user.role === 'ORGANISER';

  const editId = new URLSearchParams(window.location.search).get('id');
  const svgRef = useRef(null);
  const dragState = useRef(null);
  // Mirrors paletteDrag's toolKey for the mouseup handler below to read synchronously.
  // Kept separate from the paletteDrag *state* (which exists only to re-render the floating
  // preview's position) so handleUp can call addItem as a plain side effect rather than from
  // inside a setState updater — React 18 Strict Mode double-invokes updater functions in dev
  // specifically to catch impure ones, which doubled every drop when addItem lived in one.
  const paletteDragToolKey = useRef(null);

  const [name, setName] = useState('Untitled floor plan');
  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [gridRows, setGridRows] = useState(2);
  const [gridCols, setGridCols] = useState(6);
  const [loading, setLoading] = useState(Boolean(editId));
  const [saving, setSaving] = useState(false);
  const [note, setNote] = useState('');
  const [noteType, setNoteType] = useState('info');
  const [nameError, setNameError] = useState(false);
  const [shared, setShared] = useState(false);
  const [sharedAt, setSharedAt] = useState(null);
  const [isDefaultPlan, setIsDefaultPlan] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [boardScale, setBoardScale] = useState(1);
  // { toolKey, x, y } while a palette tool is being dragged toward the board (x/y are
  // viewport coordinates, for positioning the floating preview below); null otherwise.
  const [paletteDrag, setPaletteDrag] = useState(null);

  // Tracks the board's current on-screen scale so the floating drag preview (below) is
  // sized to match the item's real on-board size, not an arbitrary icon size.
  useEffect(() => {
    const board = svgRef.current;
    if (!board) return undefined;
    const update = () => setBoardScale(board.getBoundingClientRect().width / BOARD_W);
    update();
    const observer = new ResizeObserver(update);
    observer.observe(board);
    return () => observer.disconnect();
  }, []);

  // Load an existing plan when editing.
  useEffect(() => {
    if (!editId) return;
    let active = true;
    getFloorplans(user.userId)
      .then((plans) => {
        if (!active) return;
        const plan = (plans || []).find((p) => p.floorPlanId === editId);
        if (!plan) {
          setNoteType('warning');
          setNote('That floor plan could not be found.');
          return;
        }
        setName(plan.name || 'Untitled floor plan');
        setShared(Boolean(plan.shared));
        setSharedAt(plan.sharedAt || null);
        setIsDefaultPlan(plan.organiserId == null);
        if (plan.layoutJson) {
          try {
            const parsed = JSON.parse(plan.layoutJson);
            if (Array.isArray(parsed)) setItems(parsed);
          } catch {
            // ignore malformed layout
          }
        }
      })
      .catch((err) => { if (active) { setNoteType('warning'); setNote(err.message || 'Unable to load floor plan'); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [editId, user.userId]);

  const seatCount = items.filter((it) => SHAPES[it.type]?.counts).length;
  const hasSingle = (type) => items.some((it) => it.type === type);

  const toBoard = useCallback((clientX, clientY) => {
    const rect = svgRef.current.getBoundingClientRect();
    return {
      x: ((clientX - rect.left) / rect.width) * BOARD_W,
      y: ((clientY - rect.top) / rect.height) * BOARD_H,
    };
  }, []);

  const addItem = useCallback((toolKey, cx, cy) => {
    const tool = PALETTE_TOOLS.find((t) => t.key === toolKey);
    if (!tool) return;
    const s = SHAPES[tool.itemType];
    if (s.single && items.some((it) => it.type === tool.itemType)) {
      setNote(`Only one ${s.label.toLowerCase()} is supported.`);
      return;
    }
    const id = nextId();
    const raw = s.kind === 'rect' ? { x: cx - s.w / 2, y: cy - s.h / 2 } : { x: cx, y: cy };
    const pos = clampItem(tool.itemType, raw.x, raw.y);
    const extra = tool.tier ? { tier: tool.tier } : {};
    setItems((prev) => [...prev, { id, type: tool.itemType, ...pos, ...extra }]);
    setSelectedId(id);
    setNote('');
  }, [items]);

  function addGrid(tier) {
    const s = SHAPES.seat;
    const rows = clamp(Number(gridRows) || 1, 1, 8);
    const cols = clamp(Number(gridCols) || 1, 1, 14);
    const gap = s.r * 2 + 10;
    const startX = BOARD_W / 2 - ((cols - 1) * gap) / 2;
    const startY = BOARD_H / 2 - ((rows - 1) * gap) / 2;
    const block = [];
    for (let r = 0; r < rows; r += 1) {
      for (let c = 0; c < cols; c += 1) {
        const pos = clampItem('seat', startX + c * gap, startY + r * gap);
        block.push({ id: nextId(), type: 'seat', ...pos, tier });
      }
    }
    setItems((prev) => [...prev, ...block]);
    setNote(`Added a ${rows} x ${cols} ${tier === 'PREMIUM' ? 'premium' : 'standard'} block.`);
  }

  const onItemPointerDown = useCallback((e, item) => {
    e.stopPropagation();
    setSelectedId(item.id);
    const { x, y } = toBoard(e.clientX, e.clientY);
    dragState.current = { id: item.id, offsetX: x - item.x, offsetY: y - item.y };
  }, [toBoard]);

  // Tracks both kinds of drag with the board's own pointer events (not native HTML5
  // drag-and-drop): repositioning a placed item, and dragging a palette tool onto the
  // board. Native setDragImage hands the preview bitmap to the OS compositor, which on
  // Windows applies its own opacity/edge-feather "lifted" treatment — this stays fully
  // under our control instead, the same way in-board repositioning already did.
  useEffect(() => {
    function handleMove(e) {
      const drag = dragState.current;
      if (drag) {
        const { x, y } = toBoard(e.clientX, e.clientY);
        setItems((prev) => prev.map((it) => {
          if (it.id !== drag.id) return it;
          const pos = clampItem(it.type, x - drag.offsetX, y - drag.offsetY);
          return { ...it, ...pos };
        }));
        return;
      }
      if (paletteDragToolKey.current) {
        setPaletteDrag({ toolKey: paletteDragToolKey.current, x: e.clientX, y: e.clientY });
      }
    }
    function handleUp(e) {
      dragState.current = null;
      const toolKey = paletteDragToolKey.current;
      if (toolKey) {
        paletteDragToolKey.current = null;
        setPaletteDrag(null);
        const rect = svgRef.current?.getBoundingClientRect();
        if (rect && e.clientX >= rect.left && e.clientX <= rect.right && e.clientY >= rect.top && e.clientY <= rect.bottom) {
          const { x, y } = toBoard(e.clientX, e.clientY);
          addItem(toolKey, x, y);
        }
      }
    }
    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);
    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [toBoard, addItem]);

  const deleteSelected = useCallback(() => {
    if (!selectedId) return;
    setItems((prev) => prev.filter((it) => it.id !== selectedId));
    setSelectedId(null);
  }, [selectedId]);

  useEffect(() => {
    function handleKey(e) {
      if ((e.key === 'Delete' || e.key === 'Backspace') && selectedId) {
        e.preventDefault();
        deleteSelected();
      }
    }
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [selectedId, deleteSelected]);

  async function handleSave() {
    if (!name.trim()) {
      setNameError(true);
      setNoteType('warning');
      setNote('Give the floor plan a name before saving.');
      return;
    }
    setNameError(false);
    setSaving(true);
    setNote('');
    const payload = {
      name: name.trim(),
      layoutType: 'CUSTOM',
      capacity: seatCount,
      layoutJson: JSON.stringify(items),
    };
    try {
      if (editId) {
        await updateFloorplan(user.userId, editId, payload);
      } else {
        await createFloorplan(user.userId, payload);
      }
      navigate('/floorplans');
    } catch (err) {
      setNoteType('warning');
      setNote(err.message || 'Could not save the floor plan');
      setSaving(false);
    }
  }
  // US 13.0 / 13.3: toggles this saved plan's visibility to other organisers.
  async function handleTogglePublish() {
    if (!editId || isDefaultPlan || publishing) return;
    setPublishing(true);
    setNote('');
    try {
      const updated = shared
          ? await unpublishFloorplan(user.userId, editId)
          : await publishFloorplan(user.userId, editId);
      setShared(Boolean(updated.shared));
      setSharedAt(updated.sharedAt || null);
      setNoteType('info');
      setNote(updated.shared
          ? 'Published — other organisers can now find and reuse this floor plan.'
          : 'Unpublished. Only you can see this floor plan now.');
    } catch (err) {
      setNoteType('warning');
      setNote(err.message || 'Could not update the published state');
    } finally {
      setPublishing(false);
    }
  }

  const publishDisabledReason = !canManage
      ? 'Your organiser request is still pending approval'
      : isDefaultPlan
          ? "Starter templates can't be published"
          : !editId
              ? 'Save the floor plan first, then you can publish it'
              : undefined;

  return (
    <section className="fp">
      <div className="fp-head">
        <div>
          <p className="eyebrow">Floor Plan builder</p>
          <div className="fp-name-row">
            <input
                className={`fp-name${nameError ? ' is-invalid' : ''}`}
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  if (nameError && e.target.value.trim()) {
                    setNameError(false);
                    setNote('');
                  }
                }}
                aria-label="Floor Plan name"
                aria-invalid={nameError}
            />
            {editId && !isDefaultPlan && (
                <span className={`status-badge ${shared ? 'status-published' : 'status-draft'}`}>
                {shared ? 'Published' : 'Private'}
              </span>
            )}
          </div>
        </div>
        <div className="fp-head-actions">
          <button type="button" className="button button-secondary" onClick={() => navigate('/floorplans')} disabled={saving}>
            Back
          </button>
          {!isDefaultPlan && (
              <button
                  type="button"
                  className="button button-secondary"
                  onClick={handleTogglePublish}
                  disabled={saving || loading || publishing || !editId || !canManage}
                  title={publishDisabledReason}
              >
                {publishing ? 'Updating…' : shared ? 'Unpublish' : 'Publish'}
              </button>
          )}
          <button
              type="button"
              className="button button-primary"
              onClick={handleSave}
              disabled={saving || loading || !canManage}
              title={canManage ? undefined : 'Your organiser request is still pending approval'}
          >
            {saving ? 'Saving…' : 'Save Floor Plan'}
          </button>
        </div>
      </div>
      {shared && sharedAt && (
          <p className="fp-shared-note">
            Published on {new Date(sharedAt).toLocaleDateString()} — visible to every organiser in the shared gallery.
          </p>
      )}

      <div className="fp-workspace">
        <div className="fp-board-wrap">
          <svg
            ref={svgRef}
            className="fp-board"
            viewBox={`0 0 ${BOARD_W} ${BOARD_H}`}
            onMouseDown={() => setSelectedId(null)}
            role="application"
            aria-label="Floor Plan drawing board"
          >
            <rect x="0" y="0" width={BOARD_W} height={BOARD_H} fill="#0a2328" rx="14" />
            {Array.from({ length: Math.floor(BOARD_W / 40) - 1 }, (_, i) => (
              <line key={`v${i}`} x1={(i + 1) * 40} y1="0" x2={(i + 1) * 40} y2={BOARD_H} stroke="#123037" strokeWidth="1" />
            ))}
            {Array.from({ length: Math.floor(BOARD_H / 40) - 1 }, (_, i) => (
              <line key={`h${i}`} x1="0" y1={(i + 1) * 40} x2={BOARD_W} y2={(i + 1) * 40} stroke="#123037" strokeWidth="1" />
            ))}

            {items.map((it) => {
              const s = SHAPES[it.type];
              const selected = it.id === selectedId;
              if (s.kind === 'rect') {
                return (
                  <g key={it.id} className="fp-item" onMouseDown={(e) => onItemPointerDown(e, it)}>
                    <rect
                      x={it.x} y={it.y} width={s.w} height={s.h} rx="10"
                      fill={s.fill}
                      stroke={selected ? '#d8a84e' : 'transparent'} strokeWidth="3"
                    />
                    {it.type === 'stage' && (
                      <text
                        x={it.x + s.w / 2} y={it.y + s.h / 2 + 5} textAnchor="middle"
                        fill="#ffffff" fontSize="16" fontWeight="bold" style={{ pointerEvents: 'none' }}
                      >
                        STAGE
                      </text>
                    )}
                  </g>
                );
              }
              const fill = it.type === 'seat' ? SEAT_TIER_COLORS[seatTierOf(it)] : s.fill;
              return (
                <circle
                  key={it.id} className="fp-item"
                  cx={it.x} cy={it.y} r={s.r} fill={fill}
                  stroke={selected ? '#d8a84e' : '#0a2328'} strokeWidth={selected ? 3 : 2}
                  onMouseDown={(e) => onItemPointerDown(e, it)}
                />
              );
            })}
          </svg>
        </div>

        <aside className="fp-palette">
          <h3>Add items</h3>
          <p className="fp-hint">Drag a shape onto the board. Drag placed items to move them, and press Delete to remove the selected one.</p>

          <div className="fp-tiles">
            {PALETTE_TOOLS.map((tool) => {
              const s = SHAPES[tool.itemType];
              const disabled = s.single && hasSingle(tool.itemType);
              return (
                <div
                  key={tool.key}
                  className={`fp-tile${disabled ? ' is-disabled' : ''}`}
                  onMouseDown={(e) => {
                    if (disabled) return;
                    e.preventDefault();
                    paletteDragToolKey.current = tool.key;
                    setPaletteDrag({ toolKey: tool.key, x: e.clientX, y: e.clientY });
                  }}
                >
                  <span className="fp-tile-icon" aria-hidden="true">
                    <svg viewBox="0 0 40 40">
                      {s.kind === 'rect'
                        ? <rect x="4" y="14" width="32" height="12" rx="3" fill={tool.fill} />
                        : <circle cx="20" cy="20" r={tool.itemType === 'table' ? 15 : 11} fill={tool.fill} />}
                    </svg>
                  </span>
                  <span>{tool.label}{disabled ? ' (added)' : ''}</span>
                </div>
              );
            })}
          </div>

          <div className="fp-grid-tool">
            <h4>Seat grid</h4>
            <p className="fp-hint">Place a block of seats at once — pick standard or premium.</p>
            <div className="fp-grid-inputs">
              <label>Rows
                <input type="number" min="1" max="8" value={gridRows} onChange={(e) => setGridRows(e.target.value)} />
              </label>
              <span className="fp-times">×</span>
              <label>Cols
                <input type="number" min="1" max="14" value={gridCols} onChange={(e) => setGridCols(e.target.value)} />
              </label>
            </div>
            <div className="fp-grid-add-row">
              <button type="button" className="button button-secondary button-small" onClick={() => addGrid('STANDARD')}>
                <span className="fp-tier-dot" style={{ background: SEAT_TIER_COLORS.STANDARD }} aria-hidden="true" />
                Add standard block
              </button>
              <button type="button" className="button button-secondary button-small" onClick={() => addGrid('PREMIUM')}>
                <span className="fp-tier-dot" style={{ background: SEAT_TIER_COLORS.PREMIUM }} aria-hidden="true" />
                Add premium block
              </button>
            </div>
          </div>

          <div className="fp-stats">
            <div><strong>{seatCount}</strong><span>seats</span></div>
            <div><strong>{items.filter((it) => it.type === 'seat' && seatTierOf(it) === 'PREMIUM').length}</strong><span>premium</span></div>
            <div><strong>{items.length}</strong><span>items</span></div>
          </div>

          <div className="fp-palette-actions">
            <button type="button" className="button button-secondary button-small" onClick={deleteSelected} disabled={!selectedId}>
              Delete selected
            </button>
            <button type="button" className="button button-secondary button-small" onClick={() => { setItems([]); setSelectedId(null); }} disabled={items.length === 0}>
              Clear board
            </button>
          </div>

          {note && (
            <p className={noteType === 'warning' ? 'fp-note-warning' : 'fp-saved'} role={noteType === 'warning' ? 'alert' : 'status'}>
              {note}
            </p>
          )}
        </aside>
      </div>

      {paletteDrag && (() => {
        const tool = PALETTE_TOOLS.find((t) => t.key === paletteDrag.toolKey);
        const s = SHAPES[tool.itemType];
        const w = (s.kind === 'rect' ? s.w : s.r * 2) * boardScale;
        const h = (s.kind === 'rect' ? s.h : s.r * 2) * boardScale;
        return (
          <div
            aria-hidden="true"
            style={{
              position: 'fixed',
              left: paletteDrag.x - w / 2,
              top: paletteDrag.y - h / 2,
              width: w,
              height: h,
              background: tool.fill,
              borderRadius: s.kind === 'rect' ? '8px' : '50%',
              pointerEvents: 'none',
              opacity: 0.85,
              boxShadow: '0 6px 16px rgba(0,0,0,0.35)',
              zIndex: 1000,
            }}
          />
        );
      })()}
    </section>
  );
}
