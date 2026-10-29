# Backend (Spring Boot)

The REST API and persistence layer for Perch. A request flows through strict layers:

```text
HTTP → controller (routing, status codes, @Valid) → service (rules, @Transactional, DTO mapping)
     → repository (Spring Data JPA) → model (entities) → H2 / PostgreSQL
```

Controllers never touch repositories or entities, and only DTOs cross the HTTP boundary. Every dependency is constructor-injected.

| Package | Role | README |
|---|---|---|
| `controller/` | REST endpoints | [controller/README.md](controller/README.md) |
| `service/` | Business rules and transactions | [service/README.md](service/README.md) |
| `repository/` | Spring Data JPA interfaces | [repository/README.md](repository/README.md) |
| `model/` | JPA entities and enums | [model/README.md](model/README.md) |
| `dto/` | Request/response shapes | [dto/README.md](dto/README.md) |
| `exception/` | `ResourceNotFoundException` (404), `ValidationException` (400), `InvalidCredentialsException` (401), `SeatUnavailableException` (409), and `ApiExceptionHandler`, which turns every error into one JSON body | – |
| `config/` | `DemoAccountSeeder` and `DemoEventSeeder` (startup data), `ClockConfig` (an injectable `Clock` for testable time), `SchedulingConfig`, `BookingStatusSchemaFixer` (widens a legacy H2 enum column) | – |
| `payment/` | `PaymentProvider` interface with `MockPaymentProvider` (default) and `StripePaymentProvider`, chosen by `payment.provider` | – |
| `util/` | `PasswordUtil` (BCrypt), `PasswordValidator`, `StudentEligibility` (discount and students-only rules) | – |

`EventTicketingApplication` is the entry point.

## Running

```bash
bash gradlew bootRun
```

The API starts on `:8080`. Configuration is in `src/main/resources/application.properties`, with local secrets in the gitignored `application-local.properties`. Production uses `application-prod.properties` and PostgreSQL (see [`docs/deployment.md`](../../../../../../docs/deployment.md)).

## Adding a capability

1. Add an entity to `model/` and a repository to `repository/`. Use a UUID id, a plural `snake_case` table, and organiser-scoped finders.
2. Add request/response DTOs to `dto/`, with Bean Validation annotations on requests.
3. Put the rules in a `@Service` that throws the domain exceptions above.
4. Add a thin controller that maps to the service and sets the status code.
5. Add a service unit test (Mockito) and a controller test (MockMvc). See [`src/test/README.md`](../../../../../test/README.md).

Conventions (naming, money as `BigDecimal`, time as UTC `Instant`) are in [`CONTRIBUTING.md`](../../../../../../CONTRIBUTING.md).
