# User Story 6.0 — Live Seat Map

## Goal

As a **customer**, I want to see an event’s real seating layout with live availability, so I know exactly which seats I can choose.

The Sprint 1 event page used a fake map (`seatLayout.js`) with randomly “booked” seats. US 6 replaces that, for **published Stage events**, with seats stored in the database.

## Acceptance criteria

| Criterion | How it is met |
|---|---|
| Event details uses the organiser’s floorplan, not the Sprint 1 placeholder | Published events load seats from H2. The Stage layout is 60 seats (rows A–F). |
| Each seat shows available, held, or booked | The map colours seats from `EventSeat.status`. |
| Status comes from data, not a cached/static view | `GET /api/events/{id}/seats` reads the table on each request. The page also refreshes every 4 seconds. |
| Selecting a seat shows category and price | Hover/click shows Premium/Standard and the stored price. |

Checkout and payment are **not** part of this story. Selecting a seat is local only (cart + placeholder checkout). It does **not** hold or book the row in the database.

Custom drag-and-drop floor plans (`layout_json`) wait for **User Story 4**. This story only generates the default **Stage** layout.

---

## How a customer sees the map

```text
Organiser publishes a Stage event
        ↓
Backend writes 60 EventSeat rows (all AVAILABLE)
        ↓
Customer opens /event/{eventId}
        ↓
Frontend GET /api/events/{id}
Frontend GET /api/events/{id}/seats
        ↓
SVG map drawn from x, y, tier, price, status
```

1. Sign up / log in as a customer.
2. Browse → open a **published** event (UUID in the URL, not mock catalogue id `1`–`16`).
3. The map is the organiser’s Stage plan: 6 rows × 10 seats, aisle in the middle.
4. Hover a seat → label, category, price, and status.
5. Click an **available** seat → it goes into Your selection (frontend only).
6. Held/booked seats cannot be selected.
7. Continue to payment → placeholder checkout. **No booking is saved.**

Mock events on Browse still use the old generated map. Live H2 events use the API.

---

## Backend

### When seats are created

Seats are created **only inside `publishEvent`**, after the event is valid (floor plan, prices, banner).

`EventService.generateStageSeatsIfNeeded`:

- If that event already has seats → skip (no duplicates).
- If the floor plan is not `STAGE` → reject publish (`Seat generation is currently supported only for Stage layouts`).
- Otherwise insert 60 rows.

| Rows | Tier | Price field on the event |
|---|---|---|
| A–B (front) | `PREMIUM` | `premiumPrice` |
| C–F | `STANDARD` | `standardPrice` |

Coordinates match the organiser pricing preview (so the customer map looks the same).

Labels: `A1` … `A10`, `B1` … `F10`. All start as `AVAILABLE`.

`GET /api/events/{id}/seats` **does not** create seats. It only reads.

### Table: `event_seats`

Entity: `EventSeat`

| Column | Meaning |
|---|---|
| `event_seat_id` | UUID primary key |
| `event_id` | Which published event |
| `label` | `A1`, `C4`, … |
| `tier` | `PREMIUM` or `STANDARD` |
| `price` | Copied from the event at publish time |
| `x`, `y` | Where to draw the circle |
| `status` | `AVAILABLE`, `HELD`, or `BOOKED` |

Enums: `SeatStatus`, `SeatTier`.

### Public APIs (published events only)

| Method | Path | What it does |
|---|---|---|
| GET | `/api/events/published` | List published events |
| GET | `/api/events/{eventId}` | One published event |
| GET | `/api/events/{eventId}/seats` | Live seats for that event |

Example seat JSON:

```json
{
  "id": "…",
  "label": "A1",
  "tier": "PREMIUM",
  "price": 40.00,
  "x": 182,
  "y": 84,
  "status": "AVAILABLE"
}
```

Draft events are not exposed here. There are **no** hold/release/book endpoints in US 6.

---

## Frontend

| File | Role |
|---|---|
| `frontend/src/api/eventApi.js` | `getPublishedEvent`, `getEventSeats` |
| `frontend/src/pages/EventDetailPage.jsx` | Live map for UUID event ids |
| `frontend/src/pages/EventDetailPage.css` | Colours: teal available, orange held, grey booked |
| `frontend/src/pages/seatLayout.js` | **Only** used for mock catalogue events |

If the URL id is a UUID, the page treats it as a live event and calls the API. If it is a mock id (`1`, `2`, …), it still uses `generateLayout()`.

Colours:

- Available Premium / Standard → teal shades  
- Selected (local click) → dark green  
- Held → orange  
- Booked → grey  

The page polls `GET .../seats` every 4 seconds so if status rows change in H2, the map updates without a full reload.

---

## How to try it

1. Run backend (`./gradlew bootRun`) and frontend (`npm run dev` in `frontend/`).
2. Log in as an **organiser**.
3. Create an event → Configure Pricing (Stage layout + prices) → Publish with a banner.
4. Log in as a **customer** (or the same session if you can browse).
5. Open the published event. You should see 60 seats from H2.

Inspect seats in the H2 console (`http://localhost:8080/h2-console`):

```sql
SELECT LABEL, TIER, PRICE, STATUS, X, Y
FROM EVENT_SEATS
ORDER BY Y, X;
```

To **see** held/booked colours without a booking story, update rows in H2:

```sql
UPDATE EVENT_SEATS SET STATUS = 'HELD' WHERE LABEL = 'A2';
UPDATE EVENT_SEATS SET STATUS = 'BOOKED' WHERE LABEL = 'A1';
```

Refresh the event page. A1 should be grey, A2 orange. Those statuses are stored data, not random numbers.

---

## What this story does not do

- Does not hold a seat when you click it  
- Does not book a seat at checkout  
- Does not parse US 4 `layout_json` custom editors  
- Does not generate seats for non-Stage floor plans  

Those belong to later stories (checkout/holds, and US 4 floor-plan builder).

---

## Main files

**Create seats (publish)**  
`src/main/java/com/example/event_ticketing/service/EventService.java`

**Read seats**  
`src/main/java/com/example/event_ticketing/controller/PublicEventController.java`

**Model**  
`EventSeat.java`, `SeatStatus.java`, `SeatTier.java`, `EventSeatRepository.java`, `EventSeatDTO.java`

**UI**  
`frontend/src/pages/EventDetailPage.jsx`, `frontend/src/api/eventApi.js`
