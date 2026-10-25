// Turns an EventDTO from the API into the fields the customer screens display.

export const categories = [
  { key: 'music', label: 'Music' },
  { key: 'comedy', label: 'Comedy' },
  { key: 'theatre', label: 'Theatre' },
  { key: 'cinema', label: 'Cinema' },
  { key: 'sports', label: 'Sports' },
  { key: 'food', label: 'Food & Drink' },
  { key: 'workshop', label: 'Workshop' },
  { key: 'community', label: 'Community' },
];

export const labelFor = Object.fromEntries(categories.map((c) => [c.key, c.label]));

export function formatLocation(venueName, city) {
  const parts = [venueName, city].filter((part) => part && part.trim());
  return parts.length > 0 ? parts.join(', ') : 'Location to be announced';
}

export function formatWhen(startsAt) {
  if (!startsAt) return 'Date to be announced';
  const date = new Date(startsAt);
  if (Number.isNaN(date.getTime())) return 'Date to be announced';
  const day = new Intl.DateTimeFormat('en-AU', { weekday: 'short', day: 'numeric', month: 'short' }).format(date);
  const time = new Intl.DateTimeFormat('en-AU', { hour: 'numeric', minute: '2-digit' }).format(date);
  return `${day} · ${time}`;
}

export function toDisplayEvent(event) {
  const standard = event.standardPrice;
  return {
    ...event,
    id: event.eventId,
    cat: event.category || null,
    location: formatLocation(event.venueName, event.city),
    when: formatWhen(event.startsAt),
    price: standard !== null && standard !== undefined ? Number(standard) : null,
    universitySpecific: Boolean(event.universitySpecific),
    studentDiscountPercent: event.studentDiscountPercent || 0,
  };
}
