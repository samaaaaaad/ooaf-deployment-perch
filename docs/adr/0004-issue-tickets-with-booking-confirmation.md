# ADR 0004: Issue tickets atomically with booking confirmation

- Status: Accepted
- Date: 2026-10-18

## Context

Story 9 requires one durable, unique ticket per booked seat and safe payment webhook retries. The current implementation uses EventSeat and Booking rather than the future session/booking-item model in the logical ERD.

## Decision

Persist tickets linked to the existing booking and event seat. Enforce unique seat and ticket-code constraints. Generate UUID admission codes and render their QR images locally using ZXing. Issue tickets in the same transaction as seat booking and booking confirmation, including mock and free checkout. Lock the booking during payment callbacks; confirmed retries return the existing tickets. Reject repeated seat IDs at checkout.

## Consequences

Confirmation and My Tickets expose the same persistent ticket codes. Concurrent callback retries cannot issue extra tickets. QR rendering has no external service dependency. Existing historical confirmed bookings are not backfilled. Email delivery and check-in remain separate stories. The existing booking APIs accept customer IDs without session enforcement; this inherited authentication limitation requires separate work before production use.
