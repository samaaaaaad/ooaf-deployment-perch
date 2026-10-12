# User Story 12.0 — University / Student Discounted Events

**Status: implemented** on branch `feature/us-12-student-discount-events`.

## Goal

As a university student, I want to filter for events that are university-specific or discounted for students, so that I can find events that are relevant and affordable to me.

## Acceptance criteria

| AC | How it is met |
|----|----------------|
| Organisers can mark an event as university-specific and/or set a student discount | Create event checkbox + pricing modal (`universitySpecific`, `studentDiscountPercent` 0–100) |
| Customers can filter Browse for university-specific and/or student-discounted events | Browse chips + `GET /api/events/published?universityOnly=&studentDiscount=` |
| A discounted event shows standard and discounted prices | Browse cards, event detail, `EventDTO.studentStandardPrice` / `studentPremiumPrice` |
| Student eligibility is verified before the discount is applied | Only **admin-approved** verified students get discounts / students-only access; see below |

## Student verification

A university email alone is **not** enough. Eligibility is an explicit admin review, similar to organiser approval.

### Who counts as a student

`StudentEligibility.isEligible(User)` returns true only when `user.isStudentVerified()` is true (column `student_verified` on `users`).

Email domain (`.edu.au` / `.edu`) is only a **precondition to apply**, via `StudentEligibility.hasUniversityEmail(email)`.

### Apply → approve flow

```text
Customer (university email account)
  → Account menu: Verify student status
  → Submit: university name, student number, student-card photo (JPG/PNG/WEBP)
  → Request status PENDING

Admin (admin / admin)
  → Dashboard: pending student verifications (includes card image)
  → Approve → user.studentVerified = true  (eligible)
     or Reject → customer notified; can apply again later

Verified student
  → Students-only events: can hold seats and check out
  → Discounted events: pays reduced price (server applies %)
```

Customers can cancel a pending request or acknowledge approve/reject notifications from the account menu (`AppShell` + student-verification APIs).

### Where eligibility is enforced

| Step | Behaviour |
|------|-----------|
| Submit verification | Rejects if not university email, already verified, or already PENDING |
| Hold seat | `requireAllowedToBook` — students-only events need a **verified** student |
| Checkout | Same gate; discount applied only if verified **and** event has a discount % |
| Frontend | `UserDTO.studentEligible` mirrors `studentVerified`; UI hints to verify from account menu |

Messages point users to verify from the account menu (not “just use a .edu email”).

### Product note: students-only vs discount

- **University-specific** = students only (booking restricted to verified students).
- **Student discount** = open events where verified students pay less.

Both flags can still be set in the API (story said “and/or”). Prefer not combining them in demos: on a students-only event everyone who can book is already a student, so a separate “student % off” is usually unnecessary — set the list price instead.

## Flow (events + booking)

```text
Organiser creates / prices event
  → universitySpecific? studentDiscountPercent?
  → publish

Customer Browse
  → optional filters (university / student discount)
  → open event → see standard vs student prices

Customer books (must be verified if students-only or to get discount)
  → hold seat  → requireAllowedToBook
  → checkout   → requireAllowedToBook again
                 → if verified + discount set → apply % on server
                 → charge discounted (or full) total
```

## Key files

### Backend

| File | Role |
|------|------|
| `model/Event.java` | `universitySpecific`, `studentDiscountPercent` |
| `model/User.java` | `studentVerified` — source of truth for eligibility |
| `model/StudentVerificationRequest.java` | Pending/approved/rejected applications + card image |
| `model/StudentVerificationStatus.java` | `PENDING` / `APPROVED` / `REJECTED` / cancelled path |
| `util/StudentEligibility.java` | `isEligible(User)`, `hasUniversityEmail`, `requireAllowedToBook`, `applyDiscount` |
| `service/StudentVerificationService.java` | Submit, list pending, approve/reject, cancel, ack |
| `controller/StudentVerificationController.java` | `/api/users/{id}/student-verification…` |
| `controller/AdminController.java` | `/api/admin/student-verifications` (+ approve/reject) |
| `service/EventService.java` | Create/update fields; publish list filters; dual prices on DTO |
| `service/SeatHoldService.java` | Blocks non-verified users on students-only events |
| `service/BookingService.java` | Applies discount only after server eligibility check |
| `controller/PublicEventController.java` | `universityOnly`, `studentDiscount` query params |
| `dto/UserDTO.java` | `studentEligible` for the frontend |

### Frontend

| File | Role |
|------|------|
| `components/StudentVerificationModal.jsx` | Apply form (uni, student number, card photo) |
| `components/AppShell.jsx` | Account-menu verify / pending / cancel / outcome banners |
| `api/studentVerificationApi.js` | Customer verification endpoints |
| `api/adminApi.js` | Admin pending list + approve/reject |
| `pages/EventsPage.jsx` | Create: university-specific checkbox |
| `components/EventPricingModal.jsx` | Edit: university flag + discount % |
| `pages/BrowseEventsPage.jsx` | Filter chips; dual price / “Students only” pills |
| `pages/EventDetailPage.jsx` | Student prices; prompts to verify if blocked |
| `pages/CheckoutPage.jsx` / `MyTicketsPage.jsx` | Discount line when applied |
| `utils/studentPricing.js` | Client helpers aligned with server rounding |

### Tests

| File | Role |
|------|------|
| `service/StudentVerificationServiceTests.java` | Apply / approve / reject / university-email gate |
| `service/StudentDiscountTests.java` | Verified vs unverified booking, filters, discount totals |
| `components/StudentVerification.test.jsx` | Modal / account-menu behaviour |
| `utils/studentPricing.test.js` | Client pricing helpers |
| `pages/BrowseEventsPage.test.jsx` | Filter chips |

## Manual test (verification → book)

1. Start API: `bash gradlew bootRun`
2. Start UI: `cd frontend && npm run dev`
3. Register a customer with a **university email** (e.g. `student@uni.edu.au`) — they are **not** eligible yet.
4. Sign in → account menu → **Verify student status** → university, student number, card photo → submit.
5. Sign in as **admin** (`admin` / `admin`) → approve the pending student verification.
6. Sign back in as the student (or refresh) — `studentEligible` / verified should be true.
7. As **organiser**, publish an event with **Students only** and/or **Student discount**.
8. As the verified student: Browse → hold seats → checkout → pay (mock). Expect success; discounted total if discount was set.

**Negative checks**

- University email **without** admin approval → cannot book students-only events; no discount at checkout.
- Non-university email → cannot submit a verification request.
- Non-verified user on a **discount-only** (not students-only) event → can book at **full** price.

## Automated tests

```bash
# Backend US 12
bash gradlew test --tests 'com.example.event_ticketing.service.StudentDiscountTests'
bash gradlew test --tests 'com.example.event_ticketing.service.StudentVerificationServiceTests'

# Frontend (from frontend/)
npm test
```
