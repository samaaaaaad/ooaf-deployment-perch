# ADR 0002: Separate Events, Venues and Sessions

- Status: Accepted
- Date: 2026-08-18

## Context

An event can represent a touring production, such as a circus, with multiple performances in different cities. Treating an Event as belonging directly to one Venue would duplicate the production or prevent it from moving. A physical Venue also should not own the reusable floor-plan design.

## Decision

- `events` represents the production or tour and is owned directly by an organiser.
- `venues` represents a physical performance location and is owned directly by an organiser.
- There is no direct foreign key between Event and Venue.
- A future `event_sessions` entity will connect one Event to one Venue for a dated performance.
- EventSession will own `startsAt`, `endsAt` and its IANA timezone because performance time belongs to the session.
- Venue stores `name`, `address`, `city` and `state`; it does not store performance time.
- A future session will receive an independent copy of a selected reusable floor plan.
- Once published, the session configuration and copied layout cannot be modified.

## Consequences

- One production can have many dates and venues without duplicating Event records.
- Touring performances preserve their own time and timezone context.
- Organiser authorisation can be applied directly to Events and Venues.
- Jira #27 can implement Event and Venue independently, while EventSession and floor-plan snapshots remain later tasks.
