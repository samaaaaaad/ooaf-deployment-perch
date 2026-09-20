# User Story 6.0 — Live Seat Map

**Status: complete.** Stage maps shipped in PR #52. Custom US 4 layouts are generated from `layout_json` on publish. Hold/book on click is **not** part of this story.

## Goal

As a **customer**, I want to see an event’s real seating layout with live availability, so I know exactly which seats I can choose.

The Sprint 1 event page used a fake map (`seatLayout.js`) with randomly “booked” seats. US 6 replaces that for **published H2 events**: seats are stored in `event_seats` and drawn from the API.

## Acceptance criteria

| Criterion | Done | How |
|---|---|---|
| Event details uses the organiser’s floorplan, not the Sprint 1 placeholder | Yes | Published UUID events load seats from H2. Stage → 60 seats (`A1`–`F10`). Custom builder plans → one seat per `type=seat` item in `layout_json`. |
| Each seat shows available, held, or booked | Yes | Map colours come from `EventSeat.status` (`AVAILABLE` teal, `HELD` orange, `BOOKED` grey). |
| Status comes from data, not a cached/static view | Yes | `GET /api/events/{id}/seats` only reads the table. The page polls every 4 seconds. |
| Selecting a seat shows category and price | Yes | Hover/click shows Premium/Standard (or VIP) and the stored price. |

## Out of scope (later stories)

- Clicking a seat does **not** hold it in the database.
- Checkout does **not** book a seat.
- There are no hold / release / book APIs.

Selection is cart-only. To demo held/booked colours without a booking story, update `EVENT_SEATS.STATUS` in H2 (see below).

Mock catalogue events (`/event/1` … `/event/16`) still use `seatLayout.js`. Live events use a UUID in the URL.

---

## How a customer sees the map

```text
Organiser publishes (Stage or custom floor plan)
        ↓
Backend writes EventSeat rows (all AVAILABLE)
        ↓
Customer opens /event/{eventId}
        ↓
GET /api/events/{id}            event + layoutJson (stage/tables/barriers)
GET /api/events/{id}/seats      live seat status
        ↓
SVG map: seats at stored x,y + organiser decor
```

1. Log in as a customer.
2. Browse → open a **published** event (UUID in the URL).
3. The map is the organiser’s plan: Stage (6×10 with an aisle) or the custom builder layout.
4. Hover a seat → label, category, price, and status.
5. Click an **available** seat → it goes into Your selection (frontend only).
6. Held/booked seats cannot be selected.
7. Continue to payment → placeholder checkout. **No booking is saved.**

---

## Backend

Seats are created **only inside `publishEvent`**, after the event has a floor plan, prices, and banner.

`EventService.generateSeatsIfNeeded`:

1. If that event already has seats → skip (no duplicates).
2. If `layout_json` has `type=seat` items → one `EventSeat` per seat (`S1`, `S2`, …) at those `x`/`y`. Tables, stage, and barriers are not seats.
3. Else if the floor plan is `STAGE` → insert the 60-seat grid (`A1`–`F10`).
4. Else reject publish: `Cannot generate seats: the floor plan has no seat items`.

| Source | Labels | Tier | Price |
|---|---|---|---|
| Stage rows A–B | `A1`…`B10` | `PREMIUM` | `premiumPrice` |
| Stage rows C–F | `C1`…`F10` | `STANDARD` | `standardPrice` |
| Custom `type=seat` | `S1`, `S2`, … | `STANDARD` | `standardPrice` |

Stage coordinates match the organiser pricing preview. Custom coordinates match the editor board (900×560). All seats start as `AVAILABLE`.

`GET /api/events/{id}/seats` **does not** create seats. The published event DTO includes `layoutJson` so the customer map can draw stage, tables, and barriers.

### Table: `event_seats`

| Column | Meaning |
|---|---|
| `event_seat_id` | UUID primary key |
| `event_id` | Which published event |
| `label` | `A1`, `S1`, … |
| `tier` | `PREMIUM` or `STANDARD` |
| `price` | Copied from the event at publish time |
| `x`, `y` | Where to draw the circle |
| `status` | `AVAILABLE`, `HELD`, or `BOOKED` |

### Public APIs (published events only)

| Method | Path | What it does |
|---|---|---|
| GET | `/api/events/published` | List published events |
| GET | `/api/events/{eventId}` | One published event (includes `layoutJson`) |
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

---

## Frontend

| File | Role |
|---|---|
| `frontend/src/api/eventApi.js` | `getPublishedEvent`, `getEventSeats` |
| `frontend/src/pages/EventDetailPage.jsx` | Live map for UUID event ids |
| `frontend/src/pages/EventDetailPage.css` | Colours: teal available, orange held, grey booked |
| `frontend/src/pages/seatLayout.js` | **Only** used for mock catalogue events |

UUID in the URL → live API map. Mock id (`1`, `2`, …) → `generateLayout()`.

---

## How to try it

1. Run backend (`./gradlew bootRun`) and frontend (`npm run dev` in `frontend/`).
2. Log in as an **organiser**.
3. Create an event → Configure Pricing (Stage **or** a saved custom floor plan + prices) → Publish with a banner.
4. Open the published event as a customer. Stage shows 60 seats; a custom plan shows the seats placed in the builder.

H2 console (`http://localhost:8080/h2-console`):

```sql
SELECT LABEL, TIER, PRICE, STATUS, X, Y
FROM EVENT_SEATS
ORDER BY Y, X;
```

Held/booked colours (no booking story yet):

```sql
UPDATE EVENT_SEATS SET STATUS = 'HELD' WHERE LABEL = 'A2';
UPDATE EVENT_SEATS SET STATUS = 'BOOKED' WHERE LABEL = 'A1';
```

Refresh the event page. A1 should be grey, A2 orange.

---

## Tests

- `EventVenueServiceTests` — Stage 60-seat snapshot; custom `layout_json` seats; GET does not write; custom plan with no seats is rejected.
- `EventDetailPage.test.jsx` — live held/booked/available colours and prices; custom layout plots seats and STAGE decor from `layoutJson`.

---

## Main files

**Create seats (publish)**  
`src/main/java/com/example/event_ticketing/service/EventService.java`

**Read seats**  
`src/main/java/com/example/event_ticketing/controller/PublicEventController.java`

**Model**  
`EventSeat.java`, `SeatStatus.java`, `SeatTier.java`, `EventSeatRepository.java`, `EventSeatDTO.java`, `EventDTO.java` (`layoutJson`)

**UI**  
`frontend/src/pages/EventDetailPage.jsx`, `frontend/src/api/eventApi.js`
