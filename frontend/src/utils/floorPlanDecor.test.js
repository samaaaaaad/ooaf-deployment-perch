import { describe, expect, test } from 'vitest';

import { decorFromLayoutJson, parseLayoutItems } from './floorPlanDecor';

describe('parseLayoutItems', () => {
  test('returns an empty array for empty or missing input', () => {
    expect(parseLayoutItems('')).toEqual([]);
    expect(parseLayoutItems(null)).toEqual([]);
    expect(parseLayoutItems(undefined)).toEqual([]);
  });

  test('returns an empty array when the JSON is malformed', () => {
    expect(parseLayoutItems('not json')).toEqual([]);
    expect(parseLayoutItems('{"not":"an array"}')).toEqual([]);
  });

  test('parses a valid layout array', () => {
    const items = parseLayoutItems('[{"type":"seat","x":1,"y":2}]');
    expect(items).toHaveLength(1);
    expect(items[0]).toMatchObject({ type: 'seat', x: 1, y: 2 });
  });
});

describe('decorFromLayoutJson', () => {
  test('keeps only non-seat decor items (seats are drawn separately)', () => {
    const layout = JSON.stringify([
      { type: 'seat', x: 10, y: 10 },
      { type: 'stage', x: 80, y: 20 },
      { type: 'seat', x: 30, y: 10 },
    ]);
    const decor = decorFromLayoutJson(layout);
    expect(decor).toHaveLength(1);
    expect(decor[0].kind).toBe('stage');
  });

  test('maps a stage to a rectangle at its placed position', () => {
    const decor = decorFromLayoutJson('[{"type":"stage","x":80,"y":20}]');
    expect(decor[0]).toMatchObject({ shape: 'rect', x: 80, y: 20, w: 320, h: 64, label: 'Stage', kind: 'stage' });
  });

  test('maps tables to ellipses and numbers them T1, T2 in order', () => {
    const layout = JSON.stringify([
      { type: 'table', x: 100, y: 100 },
      { type: 'table', x: 200, y: 100 },
    ]);
    const decor = decorFromLayoutJson(layout);
    expect(decor.map((d) => d.label)).toEqual(['T1', 'T2']);
    expect(decor.every((d) => d.shape === 'ellipse')).toBe(true);
  });

  test('ignores unknown item types', () => {
    expect(decorFromLayoutJson('[{"type":"spaceship","x":1,"y":1}]')).toEqual([]);
  });
});
