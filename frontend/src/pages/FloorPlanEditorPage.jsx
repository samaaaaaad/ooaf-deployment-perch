import { useCallback, useEffect, useRef, useState } from 'react';

import { useAuth } from '../auth/AuthContext';
import { useRouter } from '../utils/router';
import './FloorPlanEditorPage.css';

// A deliberately basic floorplan builder (US 4.0 starting point). Organisers
// drag items from the palette onto the board, reposition them, and delete them.
// Two item types for now: seats (circles) and a single fixed-size stage.
// The layout is kept in the browser only; persisting coordinates to the backend
// comes later once the FloorPlan schema carries seat positions.

const BOARD_W = 800;
const BOARD_H = 520;
const SEAT_R = 13;
const STAGE_W = 320;
const STAGE_H = 64;

let idCounter = 0;
const nextId = () => {
  idCounter += 1;
  return `it-${Date.now()}-${idCounter}`;
};

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

export default function FloorPlanEditorPage() {
  const { user } = useAuth();
  const { navigate } = useRouter();
  const storageKey = `perch:floorplan:${user.userId}`;

  const svgRef = useRef(null);
  const dragState = useRef(null); // { id, offsetX, offsetY }

  const [name, setName] = useState('Untitled floorplan');
  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [savedNote, setSavedNote] = useState('');

  // Load any previously saved draft for this organiser.
  useEffect(() => {
    try {
      const raw = localStorage.getItem(storageKey);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed && Array.isArray(parsed.items)) {
          setItems(parsed.items);
          if (parsed.name) setName(parsed.name);
        }
      }
    } catch {
      // ignore corrupt or unavailable storage
    }
  }, [storageKey]);

  const seatCount = items.filter((it) => it.type === 'seat').length;
  const hasStage = items.some((it) => it.type === 'stage');

  // Convert a client (screen) point into board (viewBox) coordinates.
  const toBoard = useCallback((clientX, clientY) => {
    const rect = svgRef.current.getBoundingClientRect();
    return {
      x: ((clientX - rect.left) / rect.width) * BOARD_W,
      y: ((clientY - rect.top) / rect.height) * BOARD_H,
    };
  }, []);

  const addItem = useCallback((type, x, y) => {
    if (type === 'stage' && hasStage) {
      setSavedNote('Only one stage is supported for now.');
      return;
    }
    const id = nextId();
    if (type === 'stage') {
      const px = clamp(x - STAGE_W / 2, 0, BOARD_W - STAGE_W);
      const py = clamp(y - STAGE_H / 2, 0, BOARD_H - STAGE_H);
      setItems((prev) => [...prev, { id, type, x: px, y: py }]);
    } else {
      const px = clamp(x, SEAT_R, BOARD_W - SEAT_R);
      const py = clamp(y, SEAT_R, BOARD_H - SEAT_R);
      setItems((prev) => [...prev, { id, type, x: px, y: py }]);
    }
    setSelectedId(id);
    setSavedNote('');
  }, [hasStage]);

  // ---- Palette drag-and-drop onto the board ----
  function handleDrop(e) {
    e.preventDefault();
    const type = e.dataTransfer.getData('perch/item');
    if (!type) return;
    const { x, y } = toBoard(e.clientX, e.clientY);
    addItem(type, x, y);
  }

  // ---- Repositioning placed items by dragging them ----
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
      setItems((prev) =>
        prev.map((it) => {
          if (it.id !== drag.id) return it;
          if (it.type === 'stage') {
            return {
              ...it,
              x: clamp(x - drag.offsetX, 0, BOARD_W - STAGE_W),
              y: clamp(y - drag.offsetY, 0, BOARD_H - STAGE_H),
            };
          }
          return {
            ...it,
            x: clamp(x - drag.offsetX, SEAT_R, BOARD_W - SEAT_R),
            y: clamp(y - drag.offsetY, SEAT_R, BOARD_H - SEAT_R),
          };
        })
      );
    }
    function handleUp() {
      dragState.current = null;
    }
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

  function handleSave() {
    try {
      localStorage.setItem(storageKey, JSON.stringify({ name, items }));
      setSavedNote(`Saved · ${seatCount} seat${seatCount === 1 ? '' : 's'}`);
    } catch {
      setSavedNote('Could not save to this browser.');
    }
  }

  function handleClear() {
    setItems([]);
    setSelectedId(null);
    setSavedNote('');
  }

  return (
    <section className="fp">
      <div className="fp-head">
        <div>
          <p className="eyebrow">Floorplan builder</p>
          <input
            className="fp-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            aria-label="Floorplan name"
          />
        </div>
        <div className="fp-head-actions">
          <button type="button" className="button button-secondary" onClick={() => navigate('/events')}>
            Back to events
          </button>
          <button type="button" className="button button-primary" onClick={handleSave}>
            Save floorplan
          </button>
        </div>
      </div>

      <div className="fp-workspace">
        {/* Drawing board */}
        <div className="fp-board-wrap">
          <svg
            ref={svgRef}
            className="fp-board"
            viewBox={`0 0 ${BOARD_W} ${BOARD_H}`}
            onDragOver={(e) => e.preventDefault()}
            onDrop={handleDrop}
            onMouseDown={() => setSelectedId(null)}
            role="application"
            aria-label="Floorplan drawing board"
          >
            <rect x="0" y="0" width={BOARD_W} height={BOARD_H} fill="#0a2328" rx="14" />
            {/* subtle grid */}
            {Array.from({ length: Math.floor(BOARD_W / 40) - 1 }, (_, i) => (
              <line key={`v${i}`} x1={(i + 1) * 40} y1="0" x2={(i + 1) * 40} y2={BOARD_H} stroke="#123037" strokeWidth="1" />
            ))}
            {Array.from({ length: Math.floor(BOARD_H / 40) - 1 }, (_, i) => (
              <line key={`h${i}`} x1="0" y1={(i + 1) * 40} x2={BOARD_W} y2={(i + 1) * 40} stroke="#123037" strokeWidth="1" />
            ))}

            {items.map((it) => {
              const isSelected = it.id === selectedId;
              if (it.type === 'stage') {
                return (
                  <g
                    key={it.id}
                    className="fp-item"
                    onMouseDown={(e) => onItemPointerDown(e, it)}
                  >
                    <rect
                      x={it.x}
                      y={it.y}
                      width={STAGE_W}
                      height={STAGE_H}
                      rx="10"
                      fill="#176b63"
                      stroke={isSelected ? '#d8a84e' : 'transparent'}
                      strokeWidth="3"
                    />
                    <text
                      x={it.x + STAGE_W / 2}
                      y={it.y + STAGE_H / 2 + 5}
                      textAnchor="middle"
                      fill="#ffffff"
                      fontSize="16"
                      fontWeight="bold"
                      style={{ pointerEvents: 'none' }}
                    >
                      STAGE
                    </text>
                  </g>
                );
              }
              return (
                <circle
                  key={it.id}
                  className="fp-item"
                  cx={it.x}
                  cy={it.y}
                  r={SEAT_R}
                  fill="#3b82f6"
                  stroke={isSelected ? '#d8a84e' : '#0a2328'}
                  strokeWidth={isSelected ? 3 : 2}
                  onMouseDown={(e) => onItemPointerDown(e, it)}
                />
              );
            })}
          </svg>
        </div>

        {/* Palette */}
        <aside className="fp-palette">
          <h3>Add items</h3>
          <p className="fp-hint">Drag an item onto the board. Drag placed items to move them, and press Delete to remove the selected one.</p>

          <div
            className="fp-tile"
            draggable
            onDragStart={(e) => e.dataTransfer.setData('perch/item', 'seat')}
          >
            <span className="fp-tile-icon" aria-hidden="true">
              <svg viewBox="0 0 40 40"><circle cx="20" cy="20" r="12" fill="#3b82f6" /></svg>
            </span>
            <span>Seat</span>
          </div>

          <div
            className={`fp-tile${hasStage ? ' is-disabled' : ''}`}
            draggable={!hasStage}
            onDragStart={(e) => { if (hasStage) { e.preventDefault(); return; } e.dataTransfer.setData('perch/item', 'stage'); }}
          >
            <span className="fp-tile-icon" aria-hidden="true">
              <svg viewBox="0 0 40 40"><rect x="4" y="14" width="32" height="12" rx="3" fill="#176b63" /></svg>
            </span>
            <span>Stage{hasStage ? ' (added)' : ''}</span>
          </div>

          <div className="fp-stats">
            <div><strong>{seatCount}</strong><span>seats</span></div>
            <div><strong>{hasStage ? 1 : 0}</strong><span>stage</span></div>
          </div>

          <div className="fp-palette-actions">
            <button type="button" className="button button-secondary button-small" onClick={deleteSelected} disabled={!selectedId}>
              Delete selected
            </button>
            <button type="button" className="button button-secondary button-small" onClick={handleClear} disabled={items.length === 0}>
              Clear board
            </button>
          </div>

          {savedNote && <p className="fp-saved" role="status">{savedNote}</p>}
        </aside>
      </div>
    </section>
  );
}
