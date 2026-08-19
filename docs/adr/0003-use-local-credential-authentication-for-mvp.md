# ADR 0003: Use Local Credential Authentication for the MVP

- Status: Accepted
- Date: 2026-08-18

## Context

The project already has local registration, password hashing and login verification. Adding an external OIDC provider would introduce provider configuration, callback handling and a second identity mapping model before the MVP needs third-party API consumers or native applications.

## Decision

- Keep authentication local for the MVP.
- Store one canonical lowercase `users.email` value with a unique constraint.
- Store only a strong password hash; never store or log raw passwords.
- After successful login, establish a server-side Spring Security `HttpSession` containing the trusted user identity.
- Protected endpoints obtain the current user from the session rather than accepting a user UUID as proof of identity.
- Do not add a `user_identities` table, external OIDC provider, bearer API token flow or distributed session store in the MVP.
- Continue deriving target organiser capability from an active subscription; authentication and business authorisation remain separate concerns.

## Consequences

- The database and login flow remain small and match the existing code direction.
- Browser requests can use a secure session cookie once session enforcement is implemented.
- Email normalization must be applied consistently during registration, login and account lookup.
- The existing login endpoint currently verifies credentials but does not yet establish or enforce a session; that implementation remains a separate authentication task.
- Native applications, authorised resellers and distributed deployments will require a future authentication ADR.
