// Non-seat items (table/stage/barrier) from a floor plan's layoutJson, mapped into the
// {shape, kind, label, ...} decor objects EventDetailPage's and EventPricingModal's SVGs
// draw. Geometry mirrors FloorPlanEditorPage's SHAPES so a decor item lands in the same
// place and size it was placed in the builder.
const DECOR_GEOMETRY = {
  table: { shape: 'ellipse', rx: 26, ry: 26, label: 'Table' },
  stage: { shape: 'rect', w: 320, h: 64, label: 'Stage' },
  barrier: { shape: 'rect', w: 150, h: 16, label: 'Barrier' },
};

export function parseLayoutItems(layoutJson) {
  if (!layoutJson) return [];
  try {
    const parsed = JSON.parse(layoutJson);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function decorFromLayoutJson(layoutJson) {
  let tableCount = 0;
  return parseLayoutItems(layoutJson)
    .filter((item) => DECOR_GEOMETRY[item.type])
    .map((item) => {
      const geometry = DECOR_GEOMETRY[item.type];
      if (geometry.shape === 'ellipse') {
        tableCount += 1;
        return { shape: 'ellipse', cx: item.x, cy: item.y, rx: geometry.rx, ry: geometry.ry, label: `T${tableCount}`, kind: item.type };
      }
      return { shape: 'rect', x: item.x, y: item.y, w: geometry.w, h: geometry.h, label: geometry.label, kind: item.type };
    });
}
