# Repositories

Spring Data JPA interfaces, one per aggregate. Most queries are derived from the method name. A few use `@Query`, `@Lock` or `@EntityGraph`.

| Repository | Notable queries |
|---|---|
| `UserRepository` | `findByEmailIgnoreCase`, `existsByEmailIgnoreCase` |
| `EventRepository` | Organiser-scoped `findByEventIdAndOrganiserUserId`; `findAllByStatus…` and `findAllByOrganiser…`, both with `@EntityGraph("floorPlan")` to avoid N+1 queries |
| `VenueRepository` | Organiser-scoped list and lookup |
| `FloorPlanRepository` | `findAvailableForOrganiser` (own plans and unowned defaults), `findAllBySharedTrueOrderBySharedAtDesc` |
| `SeatRepository` | A floor plan's seats in reading order; bulk delete on re-save |
| `EventSeatRepository` | An event's seats; `releaseExpiredHolds` and `releaseHoldOf` (bulk `@Query` updates); `sumRevenue` and `findPopularEventsByTicketsSold` (admin stats, projected straight into `PopularEventDTO`) |
| `BookingRepository` | A customer's confirmed bookings; `findByPaymentReference` with `PESSIMISTIC_WRITE`, so repeated webhooks cannot double-confirm |
| `TicketRepository` | Tickets for a booking |
| `OrganiserRequestRepository` / `StudentVerificationRequestRepository` | Pending queue, latest request per user, and the "already pending" check |

## Conventions

- Lookups for organiser-owned records include the organiser UUID (`…AndOrganiserUserId`).
- Use derived query names where they stay readable; otherwise use JPQL `@Query`. Never use native SQL, so the queries run unchanged on H2 and PostgreSQL.

Tests: `src/test/java/.../repository/EventVenueRepositoryTests.java` (`@DataJpaTest` on H2) covers CRUD, ownership scoping and the fetch plan.
