# User Story 10.0 — E-Ticket Delivery

**Status: implemented** on branch `feature/us-10-eticket-email-delivery`.

## Goal

As a customer, I want my e-tickets emailed to me and available in the app, so that I can access them without relying on one device.

## Acceptance criteria

| AC | How it is met |
|----|----------------|
| Confirmed tickets are emailed to the customer's registered address | `BookingEmailService` sends HTML + plain-text mail with event, seats, **stable ticket codes**, and **inline QR images** after tickets are issued |
| Customer can view all their tickets on a "My Tickets" page | `/tickets` → `MyTicketsPage.jsx` (from US 9) |
| Each ticket displays event, seat details, and a scannable QR code | Email embeds + in-app `BookingTickets` |
| Re-viewing a ticket never regenerates a new code | Codes are persisted on `Ticket`; QR is always rendered from that same code |

## Subtasks

| ID | Work |
|----|------|
| 10.1 | Backend email delivery — HTML e-ticket email with CID QR attachments |
| 10.2 | Backend get-my-tickets — bookings API already returns `tickets[]` (US 9) |
| 10.3 | Frontend My Tickets page — already shipped (US 9) |

## Enabling real email locally

Mail is optional. With `spring.mail.*` unset, bookings still confirm and ticket codes are logged; no SMTP send happens.

Uncomment / set in `application-local.properties` (gitignored), e.g. Mailtrap:

```properties
spring.mail.host=sandbox.smtp.mailtrap.io
spring.mail.port=2525
spring.mail.username=YOUR_MAILTRAP_USERNAME
spring.mail.password=YOUR_MAILTRAP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
perch.mail.from=no-reply@perch.local
```

Then complete a checkout as a customer and check the inbox for subject `Your Perch e-tickets — …`.
