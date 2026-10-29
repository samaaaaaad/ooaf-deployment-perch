# ADR 0005: Store display location and category on events

- Status: Accepted
- Date: 2026-10-25

## Context

ADR 0002 keeps Event and Venue separate, with a future `event_sessions` table joining an Event, a Venue and a performance time. That table has not been built. Customers still need to see where an event is and filter by its category, so the frontend had been filling these in with fixed placeholder text.

## Decision

Add three optional columns to `events`: `category` (a lowercase `EventCategory` key), `venue_name` and `city`. They are display details entered when the event is created. They are not a foreign key to `venues`.

## Consequences

Browse and Event Detail show real data from the database, and organisers set it when they create an event. Venue details are duplicated as text, not normalised. When `event_sessions` is introduced, these columns should be migrated into a session's Venue, and this ADR superseded.
