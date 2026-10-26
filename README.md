# Event Ticketing System

**ELEC5619 — Object Oriented Application Frameworks**
**Project Group:** ELEC5619_Practical04_Group_1
**Repository:** Lab_Practical4_Group_1

---

## 1. Project Overview

Pop-up businesses often lack an efficient way to sell specific seating locations and instead rely on first-come, first-served arrangements. This limits their ability to price premium seats differently and manage seating layouts effectively.

Traditional ticketing platforms are typically designed for fixed venues and are poorly suited to businesses that frequently relocate or reconfigure their seating. Customising these systems can also be costly and impractical for temporary or mobile operations.

This project proposes a clean, intuitive drag-and-drop platform that allows businesses to create, modify, and rearrange seating layouts easily. Individual seats or sections can then be priced and sold independently, giving businesses greater control over seating configuration, pricing, and venue changes.

The corrected logical database model is available in the editable [Draw.io ERD](docs/event-ticketing-mvp-erd.drawio). Architecture decisions are recorded under [`docs/adr`](docs/adr), and active development conventions are documented in [`CONTRIBUTING.md`](CONTRIBUTING.md).

### 1.1 Key Requirements

Directly addressing the problems above, the platform must support:

- **Flexible layout creation:** organisers can build, edit, and rearrange a seating floorplan for any temporary or mobile venue through a drag-and-drop interface.
- **Independent seat/section pricing:** individual seats or sections can be priced separately (e.g. VIP vs. standard), rather than one flat ticket price for the whole event.
- **Real-time seat selection:** customers view live seat availability and choose a specific seat.
- **Reusable floorplans:** a layout built once for a venue can be saved and reused across multiple future events at that same pop-up location.
- **Secure payment processing:** ticket purchases are processed securely, without the platform storing raw card data.
- **Ticket delivery:** confirmed bookings generate a scannable QR e-ticket.
---

## 2. Used Libraries and Versions

Versions are the exact ones resolved by `build.gradle` (Spring Boot 4.1.0 dependency management) and installed from `frontend/package-lock.json`.

### Backend (Java 17, Spring Boot)

| Library | Version | Used for |
|---|---|---|
| Spring Boot (Gradle plugin and BOM) | 4.1.0 | Application framework, auto-configuration |
| Spring Framework (Web MVC) | 7.0.8 | REST controllers, `@RestControllerAdvice` |
| Spring Data JPA | 4.1.0 | Repositories |
| Hibernate ORM | 7.4.1.Final | JPA provider, schema generation |
| Hibernate Validator (`spring-boot-starter-validation`) | 9.1.0.Final | `@Valid` request validation |
| Spring Security Crypto | 7.1.0 | BCrypt password hashing |
| Spring Boot Actuator | 4.1.0 | `/actuator/health` for the container health check |
| Spring Boot Mail (Angus Mail) | 4.1.0 (Angus Mail 2.0.5) | E-ticket confirmation emails |
| Jackson Databind | 3.1.4 | JSON serialisation |
| Embedded Tomcat | 11.0.22 | HTTP server |
| HikariCP | 7.0.2 | Connection pool |
| H2 Database | 2.4.240 | Local file database and in-memory test database |
| PostgreSQL JDBC | 42.7.11 | Production database (`prod` profile) |
| Stripe Java | 33.4.2 | PaymentIntents and webhook signature checks |
| ZXing core / javase | 3.5.3 | QR codes on e-tickets |
| Gradle wrapper | 9.5.1 | Build tool |

**Testing:** JUnit Jupiter 6.0.3, Mockito 5.23.0, AssertJ 3.27.7, Spring Test / MockMvc 7.0.8, JsonPath 2.10.0, and JaCoCo 0.8.13 for coverage.

### Frontend (React, Vite)

| Library | Version | Used for |
|---|---|---|
| React / React DOM | 19.2.8 | UI |
| Vite | 6.4.3 | Dev server (with an `/api` proxy) and production build |
| @vitejs/plugin-react | 4.7.0 | JSX / Fast Refresh |
| @stripe/stripe-js | 10.0.0 | Loads Stripe.js for card payments |
| @stripe/react-stripe-js | 7.0.0 | Stripe Payment Element |
| Vitest | 3.2.7 | Test runner |
| React Testing Library | 16.3.2 (DOM 10.4.1, jest-dom 6.9.1) | Component tests |
| jsdom | 25.0.1 | Browser environment for tests |

The UI is hand-written React and CSS in our own teal and gold design system (section 6). No UI kit or template is used. Server communication uses `fetch` with JSON over a RESTful API (`frontend/src/api`). Seat availability refreshes by polling every 4 seconds.

---

## 3. Running the App

### 3.1 Prerequisites

- **JDK 17+.** The Gradle toolchain targets Java 17. Gradle itself is downloaded by the wrapper.
- **Node.js 18+** and npm.
- Optional: Docker, to run the production-like stack with PostgreSQL.

### 3.2 Database

No database setup is needed for local development. On first start the app creates an H2 file database at `./data/event-ticketing`, and Hibernate builds the schema from the JPA entities (`ddl-auto=update`). On every start it also seeds:

- **demo accounts** (`DemoAccountSeeder`): see [`docs/demo-logins.md`](docs/demo-logins.md)
- **16 published events** for the demo organiser (`DemoEventSeeder`): real rows with venues, upcoming dates, prices, floor plans and seats

Both seeders are idempotent. To start over, stop the app and delete `./data/`. While the app runs, you can browse the database at `http://localhost:8080/h2-console`: JDBC URL `jdbc:h2:file:./data/event-ticketing`, user `sa`, blank password.

### 3.3 Configuration and environment variables

Defaults live in `src/main/resources/application.properties`, and the app runs with none of the settings below. For local secrets, copy `src/main/resources/application-local.properties.example` to `application-local.properties`. That file is gitignored and imported automatically.

| Setting (env var) | Default | Purpose |
|---|---|---|
| `payment.provider` (`PAYMENT_PROVIDER`) | `mock` | `mock` confirms payments in-process; `stripe` uses Stripe test mode |
| `stripe.secret-key` / `stripe.publishable-key` / `stripe.webhook-secret` (`STRIPE_*`) | empty | Required only when the provider is `stripe` |
| `spring.mail.*` (`SPRING_MAIL_HOST`, …) | unset | SMTP for e-ticket emails (for example Mailtrap). Unset means the email step is skipped |
| `perch.seed.demo-events` (`PERCH_SEED_DEMO_EVENTS`) | `true` | Seed the demo event catalogue |
| `seat-hold.duration` | `PT5M` | How long a selected seat stays held |

Production (`SPRING_PROFILES_ACTIVE=prod`, PostgreSQL, Docker) is documented in [`docs/deployment.md`](docs/deployment.md).

### 3.4 Start the backend and the frontend

From the repository root, start the API on http://localhost:8080:

```bash
bash gradlew bootRun
```

In a second terminal, install the frontend dependencies (first run only):

```bash
npm --prefix frontend ci
```

Then start the frontend on http://localhost:3000. It proxies `/api` to port 8080:

```bash
npm --prefix frontend run dev
```

Open http://localhost:3000 and sign in with a demo account. `organiser@demo.local` / `Demo123!` owns the seeded events. `buyer@demo.local` / `Demo123!` can book them.

### 3.5 Tests and coverage

Run all backend tests, the coverage gate (services and controllers at 85% line coverage or higher) and the build:

```bash
bash gradlew clean build
```

Every test run writes the JaCoCo report. Open `build/reports/jacoco/test/html/index.html` in a browser; the latest figures are summarised in [`docs/coverage.md`](docs/coverage.md).

Run the frontend tests:

```bash
npm --prefix frontend test
```

---

## 4. Functionalities

Every feature below is implemented, persisted through JPA, and covered by tests.

### 4.1 Accounts and roles
- **Registration and login:** customers sign up directly. Organiser sign-ups become `PENDING_ORGANISER` until an admin approves them. Passwords are BCrypt-hashed and must meet strength rules. Bad credentials return 401.
- **Organiser requests:** an existing customer can apply to become an organiser, and cancel the request. Admins approve or reject it, and the applicant sees a one-off outcome banner.
- **Student verification (US 12):** users with a university email (`.edu`/`.edu.au`) upload a student card. An admin approves or rejects it. The card image is discarded after review.

### 4.2 Events and floor plans (organisers)
- **Venues:** save, edit and delete your pop-up locations (name, address, city, state).
- **Create events** with title, description, category, venue, city, start time and a "students only" flag. Picking a saved venue fills in the venue and city.
- **Drag-and-drop floor plan builder:** place seats, tables, a stage and barriers; save, edit and delete reusable plans. A plan used by an event cannot be deleted.
- **Shared floor plans (US 13):** publish a plan to the Community Made gallery, preview others' plans, and copy one into your own plans.
- **Pricing:** attach a floor plan and set standard and premium (VIP) prices, plus an optional student discount.
- **Publish** an event with a banner image. Seats are generated from the floor plan, and Browse hides events that have already started.

### 4.3 Booking (customers)
- **Browse** upcoming events with search, category filters and student filters.
- **Live seat map:** selecting a seat holds it on the server for 5 minutes with a countdown. Seats held or booked by others are shown as unavailable and refresh every 4 seconds. Expired holds are released by a scheduled job. Concurrent holds are protected by optimistic locking.
- **Checkout:** mock payments confirm instantly. Stripe uses PaymentIntents confirmed by a verified webhook. Free events confirm with no payment. Student discounts are applied server-side.
- **E-tickets:** one ticket with a unique code and QR image per seat, shown on the confirmation page and in My Tickets, and emailed when SMTP is configured.

### 4.4 Admin
- **Dashboard** with total revenue and the most popular events by tickets sold, plus queues for organiser requests and student verifications.

### 4.5 Out of scope
- **FR-7 ticket check-in** (scanning a ticket at the door) was dropped from the project scope. Tickets carry a unique code and QR image that a future check-in feature could validate.

---

## 5. Team Roles & Responsibilities

| SID (Name) | Role | Responsibilities |
|---|---|---|
| **520442472 (Dmitry Khachumov)** | Backend Developer — Events, Data & Payments | Database entities and relationships, event and venue management APIs, seat/section management, pricing logic, JPA repositories, and payment gateway integration |
| **540938186 (Abdul Samad)** | Full-Stack Developer — Floorplans | Organiser floorplan creation and editing, reusable floorplan functionality, frontend-backend integration for floorplans |
| **550264462 (Maria Hamlili)** | Full-Stack Developer — Events, Payments & E-tickets | Event create and list API, live seat map with database-backed availability, Stripe PaymentIntent and verified webhooks, e-ticket email confirmation, and university student verification and discounted pricing |
| **550358783 (Jose Maria Mosciaro Herrera)** | Backend Developer — Users, Bookings & Tickets | User registration and authentication, role-based access, booking APIs, and per-seat e-ticket issuance when a booking is confirmed |
| **520494057 (Thomas Huynh)** | Full-Stack Developer — Seat Holds, QR Codes & Shared Floor Plans | Organiser create-event form, seat hold and timed release with concurrency handling, QR codes on e-tickets, and shared floor-plan publishing, gallery and copy |

---

## 6. Repository Structure

Each major folder has its own README explaining its role, key files and conventions.

```text
src/main/java/com/example/event_ticketing/   Spring Boot backend            → README.md
  controller/   REST endpoints (thin)                                    → controller/README.md
  service/      Business rules, transactions, DTO mapping                → service/README.md
  repository/   Spring Data JPA repositories                             → repository/README.md
  model/        JPA entities and enums                                   → model/README.md
  dto/          Request/response shapes and the API error body           → dto/README.md
  config/       Seeders, clock, scheduling, schema fix-ups
  exception/    Domain exceptions and the global handler
  payment/      Mock and Stripe payment providers
src/main/resources/   application*.properties
src/test/             JUnit 5 / Mockito / MockMvc tests                  → src/test/README.md
frontend/             Vite + React app                                   → frontend/README.md
  src/api/          HTTP client and one module per backend resource      → frontend/src/api/README.md
  src/pages/        Routed screens                                       → frontend/src/pages/README.md
  src/components/   Shared UI pieces                                     → frontend/src/components/README.md
docs/                 ADRs, ERD, user-story notes, deployment, coverage
```

- [Backend](src/main/java/com/example/event_ticketing/README.md): [controllers](src/main/java/com/example/event_ticketing/controller/README.md), [services](src/main/java/com/example/event_ticketing/service/README.md), [repositories](src/main/java/com/example/event_ticketing/repository/README.md), [model](src/main/java/com/example/event_ticketing/model/README.md), [DTOs](src/main/java/com/example/event_ticketing/dto/README.md)
- [Backend tests](src/test/README.md)
- [Frontend](frontend/README.md): [API client](frontend/src/api/README.md), [pages](frontend/src/pages/README.md), [components](frontend/src/components/README.md)
- [Architecture decisions](docs/adr/README.md) and [contributing conventions](CONTRIBUTING.md)

---

## 7. Brand Colours

The interface uses a teal and gold palette. These are the core colours across the landing page, login and app.

| Colour | Hex | Used for |
|---|---|---|
| Teal (primary) | `#176B63` | Buttons, links, primary accents |
| Deep teal | `#0A3B37` | Hero and footer backgrounds |
| Gold (accent) | `#D8A84E` | Logo, highlights, VIP seating |
| Ink | `#17232D` | Headings and body text |
| Muted grey | `#667681` | Secondary text |
| Light background | `#F3F6F7` | Page and surface backgrounds |
