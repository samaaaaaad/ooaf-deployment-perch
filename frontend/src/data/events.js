// Shared event data for customer browse + seat-selection screens.

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

export const events = [
  { id: 1, title: 'Sunset Rooftop Sessions', cat: 'music', img: 'concert', layout: 'stage', venue: 'Rooftop Bar', city: 'Alexandria', date: 'Fri 5 Sep', time: '7:00pm', price: 35 },
  { id: 2, title: 'Thursday Night Laughs', cat: 'comedy', img: 'comedian', layout: 'cabaret', venue: 'The Comedy Loft', city: 'Newtown', date: 'Thu 4 Sep', time: '8:00pm', price: 22 },
  { id: 3, title: "A Midsummer Night's Dream", cat: 'theatre', img: 'theatre', layout: 'stage', venue: 'Old Fitz Theatre', city: 'Woolloomooloo', date: 'Sat 13 Sep', time: '7:30pm', price: 40 },
  { id: 4, title: 'Indie Film Night', cat: 'cinema', img: 'cinema', layout: 'stage', venue: 'The Vault Cinema', city: 'Surry Hills', date: 'Wed 10 Sep', time: '6:30pm', price: 18 },
  { id: 5, title: 'Inner West Futsal Cup', cat: 'sports', img: 'futsal', layout: 'arena', venue: 'Marrickville PCYC', city: 'Marrickville', date: 'Sun 21 Sep', time: '10:00am', price: 12 },
  { id: 6, title: 'Natural Wine Tasting', cat: 'food', img: 'winery', layout: 'cabaret', venue: 'The Cellar Door', city: 'Chippendale', date: 'Fri 19 Sep', time: '6:00pm', price: 55 },
  { id: 7, title: 'Intro to Pottery', cat: 'workshop', img: 'ceramics', layout: 'cabaret', venue: 'Clay & Co Studio', city: 'Erskineville', date: 'Sat 6 Sep', time: '1:00pm', price: 75 },
  { id: 8, title: 'Neighbourhood Trivia Night', cat: 'community', img: 'pub', layout: 'cabaret', venue: 'The Local Hall', city: 'Glebe', date: 'Wed 17 Sep', time: '7:00pm', price: 10 },
  { id: 9, title: 'Jazz & Small Plates', cat: 'music', img: 'saxophone', layout: 'cabaret', venue: 'Basement Bar', city: 'Sydney CBD', date: 'Sat 27 Sep', time: '8:00pm', price: 30 },
  { id: 10, title: 'Improv Jam Night', cat: 'comedy', img: 'comedyclub', layout: 'stage', venue: 'Giant Dwarf', city: 'Redfern', date: 'Fri 26 Sep', time: '8:30pm', price: 20 },
  { id: 11, title: 'One-Act Play Festival', cat: 'theatre', img: 'stage', layout: 'stage', venue: 'Kings Cross Theatre', city: 'Kings Cross', date: 'Thu 2 Oct', time: '7:00pm', price: 28 },
  { id: 12, title: 'Outdoor Movie: Amélie', cat: 'cinema', img: 'projector', layout: 'stage', venue: 'Riverside Lawn', city: 'Parramatta', date: 'Sat 4 Oct', time: '7:45pm', price: 15 },
  { id: 13, title: 'Sunrise Yoga & 5K Run', cat: 'sports', img: 'yoga', layout: 'arena', venue: 'Bondi Pavilion', city: 'Bondi Beach', date: 'Sun 5 Oct', time: '6:30am', price: 18 },
  { id: 14, title: 'Dumpling Masterclass', cat: 'food', img: 'dumpling', layout: 'cabaret', venue: 'The Kitchen Studio', city: 'Ashfield', date: 'Sun 12 Oct', time: '11:00am', price: 65 },
  { id: 15, title: 'Film Photography Basics', cat: 'workshop', img: 'camera', layout: 'cabaret', venue: 'The Darkroom', city: 'Marrickville', date: 'Sat 11 Oct', time: '2:00pm', price: 80 },
  { id: 16, title: 'Local Makers Market', cat: 'community', img: 'market', layout: 'arena', venue: 'Leichhardt Town Hall', city: 'Leichhardt', date: 'Sat 18 Oct', time: '9:00am', price: 0 },
];

export const labelFor = Object.fromEntries(categories.map((c) => [c.key, c.label]));

let dynamicEvents = [];

export function setDynamicEvents(newEvents) {
  dynamicEvents = newEvents || [];
}

export function getEvent(id) {
  const dyn = dynamicEvents.find((e) => String(e.id) === String(id));
  if (dyn) return dyn;
  return events.find((e) => String(e.id) === String(id));
}
