# Contributing

This file records the active conventions contributors should follow. Architectural decisions and their reasoning belong in `docs/adr`.

## Before Starting

1. Fetch and compare `origin/main` before merging it into a working branch.
2. Keep unrelated changes out of the same task.
3. Review the ERD and relevant ADRs before changing entities or relationships.
4. Do not commit generated H2 files from the `data` directory.

## Java Naming

- Classes, enums and interfaces use `PascalCase`: `EventService`, `EventStatus`.
- Variables, fields and methods use `camelCase`: `userId`, `findByEventId`.
- Constants use `UPPER_SNAKE_CASE`.
- Entity class names are singular: `Event`, `Venue`, `User`.
- DTOs use the entity name plus their purpose, such as `EventDTO`.
- Avoid snake_case in Java identifiers, including UUID variables.

## Database Naming and Types

- Table names are plural `snake_case`: `users`, `events`, `event_sessions`.
- Column names are `snake_case`: `user_id`, `organiser_id`, `created_at`.
- Primary and foreign keys use UUID values.
- Java `userId` maps explicitly to database column `user_id`.
- Monetary values use Java `BigDecimal` and database `DECIMAL`; never use `float` or `double`.
- Absolute timestamps use Java `Instant` and are persisted as UTC.
- A future event session owns its local IANA timezone because the performance time belongs to the session.

## Application Structure

Each backend capability follows the existing package layers:

- `model`: JPA entities and domain enums.
- `dto`: API request and response shapes. Controllers must not return entities directly.
- `repository`: Spring Data persistence and ownership-scoped queries.
- `service`: business rules, transaction boundaries and DTO mapping.
- `controller`: HTTP routing and status codes.

Use constructor injection. Keep controllers thin and do not place database access or business rules in them.

## Ownership and API Access

- `Event` and `Venue` each have a required `organiser_id` relationship to `users.user_id`.
- An Event does not directly reference a Venue. A future `event_sessions` table will connect an Event, Venue and performance time.
- Repository lookups for organiser-owned records must include the organiser UUID.
- Current organiser IDs in read routes are transitional. Write routes remain deferred until authentication supplies a trusted current user.
- Do not trust an organiser UUID supplied in a request body when creating or changing owned data.

## Authentication

- The MVP manages credentials locally; external OIDC providers and bearer API tokens are outside the current scope.
- `users.email` is the only email column. Trim and lowercase it with `Locale.ROOT` before every lookup or save.
- Enforce uniqueness on the canonical `email` value; do not add a duplicate `email_normalized` column.
- Never store or log a raw password. Persist only a strong password hash.
- Successful login must establish a server-side Spring Security session. Protected endpoints obtain the current `userId` from that session rather than from request data.
- In-memory sessions are sufficient for the single-instance MVP. A distributed session store is a future scaling decision.

## H2 Development Database

- Local development uses the file-backed H2 database configured in `src/main/resources/application.properties`.
- Tests use the isolated in-memory H2 configuration in `src/test/resources/application.properties`.
- Local schema evolution uses `spring.jpa.hibernate.ddl-auto=update` during the MVP.
- Tests use `create-drop` so mappings are checked against a clean schema.
- Do not add PostgreSQL-specific types or SQL while H2 is the accepted MVP database.

## Testing

Every code change must include or update a test that demonstrates its intended behaviour.

- Repository tests verify mappings, relationships, UUID generation and queries against H2.
- Service tests verify business behaviour independently with mocked repositories.
- Controller tests verify routes, JSON and HTTP status codes.
- Run the full suite with `bash gradlew test`.
- Run the complete verification build with `bash gradlew clean build` before opening a pull request.

## Architecture Decision Records

Create an ADR when a decision affects multiple components, establishes a lasting constraint, or would otherwise be repeatedly debated.

1. Copy the structure of an existing file in `docs/adr`.
2. Use the next sequential number and a short kebab-case name.
3. Record context, decision, consequences and status.
4. Do not rewrite an accepted ADR to hide a change. Add a new ADR that supersedes it.
