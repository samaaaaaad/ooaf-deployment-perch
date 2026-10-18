# Demo login accounts

Shared credentials for local development and demos. These accounts are created automatically on app startup by `DemoAccountSeeder` (idempotent — skipped if the email already exists).

**Do not use these passwords in production.** They are for the local H2 MVP only.

Log in at the React app (`npm run dev`) with the email and password below.

| Role | Email | Password | Notes |
|------|-------|----------|-------|
| **Admin** | `admin` | `admin` | Approves organisers and student verifications |
| **Event organiser** | `organiser@demo.local` | `Demo123!` | Can create events, floor plans, pricing |
| **Verified student** | `student@uni.sydney.edu.au` | `Demo123!` | `CUSTOMER` + `studentVerified`; gets student prices / students-only events |
| **Ticket buyer** | `buyer@demo.local` | `Demo123!` | Ordinary customer (not a student) |

## Quick copy-paste

```text
Admin:      admin / admin
Organiser:  organiser@demo.local / Demo123!
Student:    student@uni.sydney.edu.au / Demo123!
Buyer:      buyer@demo.local / Demo123!
```

## Notes

- Admin email is the literal string `admin` (not an `@` address) — that matches the existing seeder and US 12 docs.
- The student account uses a `.edu.au` address and is **already verified**, so you do not need an admin approval step to demo discounts.
- If your local H2 DB was created before these accounts were added, restart `bash gradlew bootRun` once; missing accounts are inserted on startup. To force a clean DB, stop the app and delete `./data/` (local only).
