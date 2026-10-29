# Project class architecture

These are two complementary views of the UML class diagram for the implemented Perch application. Open [class-diagrams.drawio](class-diagrams.drawio) in draw.io: its tabs are **Domain classes** and **Application classes**. [PDF](class-diagrams.pdf), [domain PNG](domain-classes.png) and [application PNG](application-classes.png) exports are included for the report. Shapes, members, labels and connectors remain editable in the draw.io file.

## Rubric and lecture alignment

The supplied `ELEC5619FinalReportMarkingGuide2026.pdf`, page 1, requires two UML diagram types: **Class and Use case**, each with an explanation. Its page 2 places the class diagram in Section 2.1 and the use case diagram in Section 2.2. These two class views address **Section 2.1**. They do not replace the separately required use case diagram; use case work is outside this change.

Lecture 3, `L3.Object-Oriented Analysis (UML).pdf`, provides the notation:

- Pages 10–11: classes divided into class name, attributes and operations.
- Page 12: `+` public and `−` private visibility.
- Page 13: numeric association multiplicities; pages 14–17 show further relationships and the ATM class example.
- Page 26: the analysis class diagram includes attributes and high-level operations. The report views select significant members rather than list every accessor or framework class.

These are UML class views, not database tables or an ERD. Associations use numeric multiplicities; hollow triangles on dashed lines represent interface realisation. Plain dashed open arrows show application dependencies. Ordinary associations avoid asserting lifecycle composition that the implementation does not establish. Parameter lists in the application view are abbreviated with `…` to keep the figure readable.

## Domain structure

The ten persistent classes represent users, access/verification requests, venues, floor plans, seats, events, event seats, bookings and tickets. All fourteen stored object-reference relationships are drawn. Roles are `UserRole` values rather than subclasses. A built-in `FloorPlan` may have no organiser; a draft `Event` may have no floor plan. Each `Seat` belongs to a reusable plan, while `EventSeat` stores event-specific price, availability, holds and the eventual booking.

`Event` now stores `category`, `venueName` and `city` as text, alongside its start time and prices. It still has no `Venue` association. `EventSeat` has no stored `Seat` association. `Booking.seatIds` captures the pending checkout selection; confirmed inventory links to the booking and gets one unique `Ticket` per sold seat. Optional multiplicities allow unsold seats and bookings that have not been confirmed.

## Application structure

The application view shows all eleven services/jobs and the payment interface with its two implementations. `EventService` manages events, pricing, reusable floor plans and gallery sharing. `VenueService` remains read-only in this source snapshot. Registration and review services manage credentials, organiser requests and student verification; `AdminService` supplies revenue/popularity summaries.

`SeatHoldService` applies temporary holds to versioned `EventSeat` records. `ExpiredSeatHoldReleaseJob` frees expired holds. `BookingService` validates held seats and coordinates `PaymentProvider`, `TicketService` and `BookingEmailService`. Mock payment confirms locally; Stripe mode waits for a verified webhook. Ticket creation shares the confirmation transaction, and email is optional and best effort.

React functional components form the UI; REST controllers delegate to the services, and Spring Data repositories persist the domain objects. DTOs cross the HTTP boundary. These repetitive controller/repository/DTO classes, enums and configuration/utilities are abstracted in this report view. The class figures do not claim server-side session enforcement: the current API uses supplied IDs and business role/ownership checks.

## Verification and regeneration

`class-inventory.json` records the inspected source commit and SHA-256 hashes of Java source files. It is a source snapshot, not the future commit containing the exported documentation. The checker verifies all displayed member names, every JPA association and multiplicity, selected dependencies, interface implementations, class coverage and XML endpoints/page bounds.

From the repository root:

```bash
python3 docs/architecture/build_class_diagrams.py
python3 docs/architecture/verify_class_diagrams.py
```

The generator only produces class views. Export tab 1 and tab 2 to PNG and all tabs to PDF in draw.io after regenerating. The exports have been visually inspected. No use case diagram was generated or changed.
