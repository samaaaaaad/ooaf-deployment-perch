# User Story 12.0 — University / Student Discounted Events

**Status: implemented** on branch `feature/us-12-student-discount-events` (commit `feat: university / student discounted events (US 12)`). Not merged to `main` until the PR lands.

## Goal

As a university student, I want to filter for events that are university-specific or discounted for students, so that I can find events that are relevant and affordable to me.

## Acceptance criteria

| AC | How it is met |
|----|----------------|
| Organisers can mark an event as university-specific and/or set a student discount | Create event checkbox + pricing modal (`universitySpecific`, `studentDiscountPercent` 0–100) |
| Customers can filter Browse for university-specific and/or student-discounted events | Browse chips + `GET /api/events/published?universityOnly=&studentDiscount=` |
| A discounted event shows standard and discounted prices | Browse cards, event detail, `EventDTO.studentStandardPrice` / `studentPremiumPrice` |
| Student eligibility is verified before the discount is applied | Server checks email domain (`.edu.au` / `.edu`) at hold and checkout; client never sets the paid total |

## Eligibility (interim)

MVP rule in `StudentEligibility`:

- Eligible if the account email’s domain ends with `.edu.au` or `.edu`
- University-specific events: **only** eligible customers can hold seats or check out
- Student discount: eligible customers pay the reduced price; others pay full price

Swap the body of `isEligible()` later if the team picks a stronger verification method; hold/checkout already call that helper.

## Flow

```text
Organiser creates / prices event
  → universitySpecific? studentDiscountPercent?
  → publish

Customer Browse
  → optional filters (university / student discount)
  → open event → see standard vs student prices

Customer books
  → hold seat  → requireAllowedToBook (students-only gate)
  → checkout   → requireAllowedToBook again
                 → if eligible + discount set → apply % on server
                 → charge discounted (or full) total
```

## Key files

### Backend

| File | Role |
|------|------|
| `model/Event.java` | `universitySpecific`, `studentDiscountPercent` |
| `model/Booking.java` | Stores applied discount %; `pricePaidFor(seat)` |
| `util/StudentEligibility.java` | `isEligible`, `requireAllowedToBook`, `applyDiscount` |
| `service/EventService.java` | Create/update fields; publish list filters; dual prices on DTO |
| `service/SeatHoldService.java` | Blocks non-students on students-only events |
| `service/BookingService.java` | Applies discount only after server eligibility check |
| `controller/PublicEventController.java` | `universityOnly`, `studentDiscount` query params |
| `dto/EventDTO.java`, `CreateEventRequest.java`, `UpdatePricingRequest.java` | API shape |
| `dto/UserDTO.java` | `studentEligible` for the frontend |
| `dto/BookingDTO.java` | Exposes applied discount on confirmed booking |

### Frontend

| File | Role |
|------|------|
| `pages/EventsPage.jsx` | Create: university-specific checkbox |
| `components/EventPricingModal.jsx` | Edit: university flag + discount % |
| `pages/BrowseEventsPage.jsx` | Filter chips; dual price / “Students only” pills |
| `api/eventApi.js` | Passes filter query params |
| `pages/EventDetailPage.jsx` | Shows student prices; blocks hold if not eligible |
| `pages/CheckoutPage.jsx` | Shows discount line when applied |
| `pages/MyTicketsPage.jsx` | Shows “student discount applied” on booking |
| `utils/studentPricing.js` | Client helpers aligned with server rounding |

### Tests

| File | Role |
|------|------|
| `service/StudentDiscountTests.java` | Eligibility, filters, discount at checkout, students-only hold/checkout |
| `utils/studentPricing.test.js` | Client pricing / eligibility helpers |
| `pages/BrowseEventsPage.test.jsx` | Filter chips |
| `pages/EventDetailPage.test.jsx` | Students-only messaging (partial) |

## Manual test (student verification → book)

1. Start API: `bash gradlew bootRun`
2. Start UI: `cd frontend && npm run dev`
3. As **organiser**, create a published event with:
   - **University-specific** on (students only), and/or
   - **Student discount** e.g. 20%
4. Register / log in as a **student**: email ending in `.edu.au` or `.edu` (e.g. `sam@student.uts.edu.au`)
5. Browse → open the event → hold seats → checkout → pay (mock)
6. Confirm the charged total is the discounted amount (if discount set) and booking succeeds

**Negative checks**

- Log in as `someone@gmail.com` on a **university-specific** event → hold/checkout should fail with the students-only message
- Same non-student on a **discount-only** (not students-only) event → can book, but pays **full** price

## Automated tests

```bash
# Backend US 12
bash gradlew test --tests 'com.example.event_ticketing.service.StudentDiscountTests'

# Frontend (from frontend/)
npm test
```
