# ADR 0001: Use H2 for MVP Development and Testing

- Status: Accepted
- Date: 2026-08-18

## Context

The MVP data model is still changing. The team needs a database that runs locally without installing or managing a separate database server. The application also needs repeatable persistence tests that exercise real JPA mappings rather than repository mocks alone.

## Decision

Use H2 as the only database for the current MVP:

- Local development uses a file-backed database at `./data/event-ticketing`.
- Hibernate uses `ddl-auto=update` locally while the model is evolving.
- Tests use an isolated in-memory H2 database with `ddl-auto=create-drop`.
- Generated local database files are excluded from Git.
- PostgreSQL and Flyway are not included at this stage.

## Consequences

- Every contributor can run the backend and persistence tests without a separate database service.
- Repository tests validate JPA mappings, UUID keys and relationships against a real relational database.
- `ddl-auto=update` is convenient but does not provide controlled, reviewable production migrations.
- Before production deployment, the team must reconsider the database, migration strategy, backup requirements and concurrency behaviour. A future change must be recorded in a new ADR.
