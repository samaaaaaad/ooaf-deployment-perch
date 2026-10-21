# Deployment

This describes how to run Perch in production. The app is packaged as a **single container**: the
React frontend is built and bundled into the Spring Boot jar, so the UI and the API are served from
one process at one URL. There is no separate frontend host and no CORS boundary.

Local development is unchanged: `./gradlew bootRun` + `npm run dev` still use H2 and the Vite proxy.
Production is a separate Spring profile (`prod`) that uses PostgreSQL.

## What changes in production

- **Database:** PostgreSQL instead of the local H2 file. H2 writes to the container disk, which is
  wiped on every redeploy; a managed Postgres keeps data durable across restarts and deploys.
- **Schema:** Hibernate creates the schema from the entities on the fresh database
  (`ddl-auto=update`). This is safe because production starts empty, so there is no legacy
  constraint to migrate. See *Hardening follow-ups* for moving to Flyway.
- **H2 console:** disabled.
- **Config and secrets:** all supplied as environment variables; nothing secret is committed.

## Build artifacts

The Docker image builds both halves itself, so a deploy needs only the repo:

```bash
docker build -t perch .
```

To build the pieces by hand instead:

```bash
./gradlew bootJar
```

```bash
cd frontend && npm run build
```

## Run the whole stack locally (production parity)

Brings up the app plus Postgres on http://localhost:8080 with a persistent volume:

```bash
docker compose up --build
```

## Environment variables

| Variable | Required | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | Must be `prod`. |
| `SPRING_DATASOURCE_URL` | yes | e.g. `jdbc:postgresql://host:5432/perch`. |
| `SPRING_DATASOURCE_USERNAME` | yes | Database user. |
| `SPRING_DATASOURCE_PASSWORD` | yes | Database password. |
| `PAYMENT_PROVIDER` | no | `mock` (default) or `stripe`. |
| `STRIPE_SECRET_KEY` | if stripe | Stripe **test-mode** secret key. |
| `STRIPE_PUBLISHABLE_KEY` | if stripe | Stripe publishable key. |
| `STRIPE_WEBHOOK_SECRET` | if stripe | Signing secret for the webhook endpoint. |
| `PERCH_MAIL_FROM` | no | From address on confirmation emails. |
| `SPRING_MAIL_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | no | SMTP (e.g. Mailtrap). Blank = email step skipped. |

Secrets are entered into the host's own environment-variable settings, never committed to the repo.

## Deploying to a host (Render shown; any Docker host is the same shape)

1. Create a **PostgreSQL** instance on the host. Copy the connection URL, user and password it gives you.
2. Create a **Web Service** from this repo; it auto-detects the `Dockerfile`.
3. Set the environment variables from the table above (`SPRING_PROFILES_ACTIVE=prod`, the three
   `SPRING_DATASOURCE_*` from step 1, and the Stripe/mail vars if used).
4. Set the health check path to **`/actuator/health`**.
5. Deploy. The frontend and API are both served from the service's public URL.

Because it is a plain container, the same image runs unchanged on AWS, GCP, Fly.io, or a VM later.

## Stripe webhook

Stripe needs a public HTTPS URL. After the service is live, add the webhook endpoint in the Stripe
dashboard pointing at `https://<your-service-url>/api/stripe/webhook` (confirm the exact path in
`StripeWebhookController`), then put its signing secret in `STRIPE_WEBHOOK_SECRET`. Keep Stripe in
**test mode** for demos. For a local demo without a public URL, use the Stripe CLI or an ngrok tunnel.

## Hardening follow-ups (recommended before a real public launch)

- **Server-side auth.** The API currently trusts user ids sent by the client. Add authentication
  before exposing it publicly.
- **Flyway migrations.** Replace `ddl-auto=update` with versioned Flyway migrations and
  `ddl-auto=validate`, so schema changes are reviewable and repeatable. Note: the large base64 text
  columns (`events.image_url`, `events.description`, `floor_plans.layout_json`,
  `student_verification_requests.card_image`) map to Postgres large objects (`oid`) by default;
  annotate them to store as `text` when introducing the migration.
- **Object storage for images.** Base64 images live in the database today, which is simple and
  consistent but heavy. Moving them to object storage (e.g. S3) with only the URL in the DB scales better.
