import { useCallback, useEffect, useRef, useState } from 'react';

import { createFloorplan, getFloorplans, updateFloorplan } from '../api/eventApi';
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
  seat: { kind: 'circle', r: 13, fill: '#3b82f6', label: 'Seat', counts: true },
  table: { kind: 'circle', r: 26, fill: '#14544d', label: 'Table', counts: false },
  stage: { kind: 'rect', w: 320, h: 64, fill: '#176b63', label: 'Stage', single: true },
  barrier: { kind: 'rect', w: 150, h: 16, fill: '#4b5563', label: 'Barrier', single: false },
};

let idCounter = 0;
const nextId = () => {
  idCounter += 1;
  return `it-${Date.now()}-${idCounter}`;
};

const clamp = (v, min, max) => Math.min(Math.max(v, min), max);
const isRect = (type) => SHAPES[type].kind === 'rect';

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

  const addItem = useCallback((type, cx, cy) => {
    const s = SHAPES[type];
    if (s.single && hasSingle(type)) {
      setNote(`Only one ${s.label.toLowerCase()} is supported.`);
      return;
    }
    const id = nextId();
    const raw = s.kind === 'rect' ? { x: cx - s.w / 2, y: cy - s.h / 2 } : { x: cx, y: cy };
    const pos = clampItem(type, raw.x, raw.y);
    setItems((prev) => [...prev, { id, type, ...pos }]);
    setSelectedId(id);
    setNote('');
  }, [items]);

  function addGrid() {
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
        block.push({ id: nextId(), type: 'seat', ...pos });
      }
    }
    setItems((prev) => [...prev, ...block]);
    setNote(`Added a ${rows} x ${cols} block.`);
  }

  function handleDrop(e) {
    e.preventDefault();
    const type = e.dataTransfer.getData('perch/item');
    if (!SHAPES[type]) return;
    const { x, y } = toBoard(e.clientX, e.clientY);
    addItem(type, x, y);
  }

  const onItemPointerDown = useCallback((e, item) => {
    e.stopPropagation();
    setSelectedId(item.id);
    const { x, y } = toBoard(e.clientX, e.clientY);
    dragState.current = { id: item.id, offsetX: x - item.x, offsetY: y - item.y };
  }, [toBoard]);

  useEffect(() => {
    function handleMove(e) {
      const drag = dragState.current;
      if (!drag) return;
      const { x, y } = toBoard(e.clientX, e.clientY);
      setItems((prev) => prev.map((it) => {
        if (it.id !== drag.id) return it;
        const pos = clampItem(it.type, x - drag.offsetX, y - drag.offsetY);
        return { ...it, ...pos };
      }));
    }
    function handleUp() { dragState.current = null; }
    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);
    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [toBoard]);

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

  const paletteTypes = ['seat', 'table', 'stage', 'barrier'];

  return (
    <section className="fp">
      <div className="fp-head">
        <div>
          <p className="eyebrow">Floor Plan builder</p>
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
        </div>
        <div className="fp-head-actions">
          <button type="button" className="button button-secondary" onClick={() => navigate('/floorplans')} disabled={saving}>
            Back
          </button>
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

      <div className="fp-workspace">
        <div className="fp-board-wrap">
          <svg
            ref={svgRef}
            className="fp-board"
            viewBox={`0 0 ${BOARD_W} ${BOARD_H}`}
            onDragOver={(e) => e.preventDefault()}
            onDrop={handleDrop}
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
              return (
                <circle
                  key={it.id} className="fp-item"
                  cx={it.x} cy={it.y} r={s.r} fill={s.fill}
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
            {paletteTypes.map((type) => {
              const s = SHAPES[type];
              const disabled = s.single && hasSingle(type);
              return (
                <div
                  key={type}
                  className={`fp-tile${disabled ? ' is-disabled' : ''}`}
                  draggable={!disabled}
                  onDragStart={(e) => {
                    if (disabled) { e.preventDefault(); return; }
                    e.dataTransfer.setData('perch/item', type);
                  }}
                >
                  <span className="fp-tile-icon" aria-hidden="true">
                    <svg viewBox="0 0 40 40">
                      {s.kind === 'rect'
                        ? <rect x="4" y="14" width="32" height="12" rx="3" fill={s.fill} />
                        : <circle cx="20" cy="20" r={type === 'table' ? 15 : 11} fill={s.fill} />}
                    </svg>
                  </span>
                  <span>{s.label}{disabled ? ' (added)' : ''}</span>
                </div>
              );
            })}
          </div>

          <div className="fp-grid-tool">
            <h4>Seat grid</h4>
            <div className="fp-grid-inputs">
              <label>Rows
                <input type="number" min="1" max="8" value={gridRows} onChange={(e) => setGridRows(e.target.value)} />
              </label>
              <span className="fp-times">×</span>
              <label>Cols
                <input type="number" min="1" max="14" value={gridCols} onChange={(e) => setGridCols(e.target.value)} />
              </label>
            </div>
            <button type="button" className="button button-secondary button-small" onClick={addGrid}>
              Add {clamp(Number(gridRows) || 1, 1, 8)} × {clamp(Number(gridCols) || 1, 1, 14)} block
            </button>
          </div>

          <div className="fp-stats">
            <div><strong>{seatCount}</strong><span>seats</span></div>
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
    </section>
  );
}
