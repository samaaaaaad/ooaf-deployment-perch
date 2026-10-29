import { formatLocation, formatWhen, labelFor, toDisplayEvent } from './eventDisplay';

test('joins the venue and city, skipping blanks', () => {
  expect(formatLocation('Basement Bar', 'Sydney CBD')).toBe('Basement Bar, Sydney CBD');
  expect(formatLocation('Basement Bar', '')).toBe('Basement Bar');
  expect(formatLocation(null, 'Glebe')).toBe('Glebe');
  expect(formatLocation(null, null)).toBe('Location to be announced');
});

test('formats the start time, or says it is not announced', () => {
  expect(formatWhen('2027-03-06T09:30:00Z')).toMatch(/Mar/);
  expect(formatWhen(null)).toBe('Date to be announced');
  expect(formatWhen('not a date')).toBe('Date to be announced');
});

test('maps an API event into the display shape', () => {
  const display = toDisplayEvent({
    eventId: 'e1', title: 'Jazz', category: 'music', venueName: 'Basement Bar', city: 'Sydney CBD',
    startsAt: null, standardPrice: '30.00', universitySpecific: false, studentDiscountPercent: null,
  });
  expect(display).toMatchObject({
    id: 'e1', cat: 'music', location: 'Basement Bar, Sydney CBD', when: 'Date to be announced',
    price: 30, universitySpecific: false, studentDiscountPercent: 0,
  });
  expect(labelFor[display.cat]).toBe('Music');
});

test('leaves the price unset when the event has no pricing', () => {
  expect(toDisplayEvent({ eventId: 'e2', standardPrice: null }).price).toBeNull();
});
