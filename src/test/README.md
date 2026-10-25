# Backend tests

JUnit 5 tests for every backend layer, run against an isolated in-memory H2 database (`src/test/resources/application.properties`: `create-drop` schema, demo event seeding off, slow hold-release interval).

```bash
bash gradlew test
```

```bash
bash gradlew clean build
```

`clean build` also runs the JaCoCo coverage gate. Open the report at `build/reports/jacoco/test/html/index.html`; the summary is in [`docs/coverage.md`](../../docs/coverage.md).

| Folder | Style | What it proves |
|---|---|---|
| `controller/` | Standalone **MockMvc** with a **Mockito**-mocked service and `ApiExceptionHandler` | Routes, HTTP verbs, status codes, JSON fields, `@Valid` rejections, error bodies. `StripeWebhook*` tests send correctly and incorrectly signed webhook payloads |
| `service/` | Plain **Mockito** unit tests (`@ExtendWith(MockitoExtension.class)`), plus a few `@SpringBootTest` ones | Business rules and their error paths: registration, organiser and student requests, pricing and publishing, floor plans, holds, checkout, payment callbacks, discounts, email |
| `repository/` | `@DataJpaTest` on H2 | Mappings, UUID keys, CRUD, organiser scoping, and that list queries fetch floor plans in one query |
| `integration/` | `@SpringBootTest` (`@Transactional` where possible) | Real database flows end to end: checkout with tickets, concurrent seat holds, hold expiry, floor plan seat generation, event scheduling, and the demo seeder |
| `exception/` | MockMvc on a throw-only controller | Each exception type maps to the right status and the JSON error shape |

## Conventions

- Name a test as a sentence: `rejectsACheckoutWithNoSeatsAndReportsLostHoldsAsConflicts`.
- Every code change includes or updates a test (see `CONTRIBUTING.md`).
- Inject a fixed `Clock` instead of sleeping when time matters.
