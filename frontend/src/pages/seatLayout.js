// Generates a seat plan for an event. The arrangement is seeded by the event
// id so every event gets a consistent but different layout. Three styles:
//   stage   - rows of seats facing a stage (shows, gigs, cinema)
//   cabaret - round tables with seats around them (comedy, dining, workshops)
//   arena   - a central court/floor with stands on either side (sports, markets)

function makeRng(seed) {
  let a = (seed >>> 0) || 1;
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function priceTiers(event) {
  const base = event.price > 0 ? event.price : 15;
  return { vip: base * 2, premium: Math.round(base * 1.5), standard: base };
}

function stage(event, price, rng) {
  const decor = [{ shape: 'rect', x: 200, y: 20, w: 240, h: 40, label: 'Stage', kind: 'stage' }];
  const seats = [];
  const rowLetters = 'ABCDEF';
  for (let r = 0; r < 6; r += 1) {
    for (let c = 0; c < 10; c += 1) {
      const aisle = c >= 5 ? 24 : 0;
      const tier = r < 2 ? 'vip' : r < 4 ? 'premium' : 'standard';
      seats.push({
        id: `${rowLetters[r]}${c + 1}`, label: `${rowLetters[r]}${c + 1}`,
        x: 182 + c * 26 + aisle, y: 104 + r * 34,
        tier, price: price[tier], booked: rng() < 0.16,
      });
    }
  }
  return { width: 640, height: 300, decor, seats };
}

function cabaret(event, price, rng) {
  const decor = [{ shape: 'rect', x: 244, y: 16, w: 152, h: 30, label: 'Stage', kind: 'stage' }];
  const seats = [];
  const xs = [150, 320, 490];
  const ys = [148, 320];
  let t = 0;
  ys.forEach((cy, ty) => {
    xs.forEach((cx) => {
      t += 1;
      decor.push({ shape: 'ellipse', cx, cy, rx: 42, ry: 28, label: `T${t}`, kind: 'table' });
      const tier = ty === 0 ? 'premium' : 'standard';
      for (let s = 0; s < 6; s += 1) {
        const ang = (Math.PI * 2 * s) / 6 - Math.PI / 2;
        seats.push({
          id: `T${t}-${s + 1}`, label: `T${t} · ${s + 1}`,
          x: cx + Math.cos(ang) * 66, y: cy + Math.sin(ang) * 50,
          tier, price: price[tier], booked: rng() < 0.15,
        });
      }
    });
  });
  return { width: 640, height: 400, decor, seats };
}

function arena(event, price, rng) {
  const label = event.cat === 'sports' ? 'Court' : 'Floor';
  const decor = [{ shape: 'rect', x: 168, y: 188, w: 304, h: 108, label, kind: 'court' }];
  const seats = [];
  const row = (y, tier, prefix, idx) => {
    for (let c = 0; c < 12; c += 1) {
      seats.push({
        id: `${prefix}${idx}-${c + 1}`, label: `${prefix}${idx} · ${c + 1}`,
        x: 160 + c * 28, y, tier, price: price[tier], booked: rng() < 0.15,
      });
    }
  };
  [70, 100, 130].forEach((y, i) => row(y, i === 2 ? 'premium' : 'standard', 'N', i + 1));
  [354, 384, 414].forEach((y, i) => row(y, i === 0 ? 'premium' : 'standard', 'S', i + 1));
  return { width: 640, height: 462, decor, seats };
}

export function generateLayout(event) {
  const price = priceTiers(event);
  const rng = makeRng(event.id * 97 + 13);
  if (event.layout === 'cabaret') return cabaret(event, price, rng);
  if (event.layout === 'arena') return arena(event, price, rng);
  return stage(event, price, rng);
}
