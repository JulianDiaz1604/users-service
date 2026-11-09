# CLAUDE.md — Plazoleta de comidas (PRAGMA)

Project conventions for every microservice of the "Plazoleta de comidas" challenge.
These rules are shared across all repositories. The specific microservice this repo
implements is described in `README.md`.

## 1. What we are building

A food-court ordering backend split into **four independent microservices** (one repo each):

| Repo | Responsibility | Datastore |
|------|----------------|-----------|
| `users-service` | Users, roles, authentication (JWT) | PostgreSQL |
| `food-court-service` | Restaurants, dishes, orders | PostgreSQL |
| `messaging-service` | SMS notifications + security PIN (AWS SNS) | PostgreSQL |
| `traceability-service` | Order status history & efficiency metrics | DynamoDB |

Order lifecycle: `PENDING → IN_PREPARATION → READY → DELIVERED`, plus `PENDING → CANCELLED`.
A client may have only **one active order** at a time.

## 2. Architecture — Hexagonal (ports & adapters). NON-NEGOTIABLE.

Three layers, dependencies point **inward only**: `infrastructure → application → domain`.

- **domain** — pure business logic. No Spring, no JPA, no framework imports. Contains:
  `api` (driving/primary port interfaces), `spi` (driven/secondary port interfaces),
  `model` (domain models), `usecase` (use cases: plain classes implementing `api` ports),
  `validation` (domain validators), `exception` (domain exceptions), `util` (constants).
- **application** — orchestration. `dto/request`, `dto/response`, `handler` + `handler/impl`
  (`@Service`), `mapper` (MapStruct). May depend on **domain only**.
- **infrastructure** — technical adapters. `configuration` (`BeanConfiguration` wires use cases
  and adapters as `@Bean`), `documentation` (OpenAPI), `exceptionhandler` (`ControllerAdvisor`),
  `input/rest` (controllers), `out/jpa` | `out/dynamo` (adapters, entities, mappers, repositories).

Hard rules:
- **domain depends on nothing.** No `org.springframework.*`, no `jakarta.persistence.*` in domain.
- **application must NOT import any `infrastructure` class.**
- Cross-layer wiring is manual in `BeanConfiguration`; only `*Handler` carries `@Service`.
- **Cross-service checks** (e.g. validating an owner's role) live in the **use case behind an SPI
  port**, never in a pure validator (validators stay port-free and pure).
- Swap a datastore or transport without touching domain/application.

## 3. Tech stack (current, stable)

- **Java 21** (LTS, toolchain-pinned) · **Spring Boot 3.5.x** · **Gradle 8.10.x** (wrapper)
- **PostgreSQL** (relational) / **DynamoDB** (traceability)
- **MapStruct 1.6.x** (mappers) · **Lombok** (Boot-managed) · **springdoc-openapi 2.x** (Swagger UI)
- **JUnit 5 + Mockito** (tests) · **JaCoCo** (coverage) · **spring-security-crypto** (bcrypt)
- Spring Boot 3 uses the **`jakarta.*`** namespace (never `javax.*`).

## 4. Coding conventions

- **Language: full English** — identifiers, domain model, comments, commits, docs. Domain terms too
  (`Restaurant`, `Dish`, `Order`, `Owner`, `Employee`, `OrderStatus`). No Spanish in code.
- Naming: packages `lowercase`; classes/interfaces `PascalCase`; methods/vars/params `camelCase`;
  constants `UPPER_SNAKE_CASE`; booleans affirmative (`isActive`, `existsOwner`, `hasPin`).
- Interfaces are `I`-prefixed (`IUserServicePort`, `IUserPersistencePort`, mappers, repositories).
- Class suffixes: `*Model`, `*UseCase`, `*Handler`, `*RequestDto`, `*ResponseDto`, `*RestController`,
  `*JpaAdapter`, `*Entity`, `*Repository`, `*Mapper`, `*Validator`.
- Constructor injection via `@RequiredArgsConstructor`. **No field injection.**
- **No wildcard imports.** SOLID. Short, single-purpose methods. Self-explanatory names over comments.
- MapStruct: `@Mapper(componentModel = "spring", unmappedTargetPolicy = IGNORE)`.
- Passwords hashed with **bcrypt**. Validate per each HU's acceptance criteria (email format,
  phone ≤ 13 chars allowing `+`, numeric document, positive price, adult age, etc.).
- **Entity identifiers use `java.util.UUID`, never `Long`.** Every aggregate/entity `id` and every
  reference to one (e.g. `ownerId`) is a `UUID`, across domain models, entities, DTOs, ports,
  repositories (`JpaRepository<Entity, UUID>`), controller `@PathVariable`s and tests. JPA entities
  generate it with `@GeneratedValue(strategy = GenerationType.UUID)`. This is the general rule for
  all new entities/objects from now on. Exception: enum discriminators that are not entity PKs
  (e.g. `roleId`, which maps to the `UserRole` enum) keep their natural type.

## 5. Testing — TDD, mandatory per HU

- Write the test first (red → green → refactor). Every HU ships with unit tests.
- JUnit 5 + Mockito: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`,
  `when(...).thenReturn(...)`, fixtures in `@BeforeEach`.
- **FIRST**: Fast, Independent, Repeatable, Self-validating, Timely. No external deps in unit tests.
- Unit-test the domain validators and use cases; name tests after the behavior under test.
- **Test method names in camelCase BDD style (never snake_case)** to satisfy Sonar rule **S100**.

## 6. Git workflow

- Integration branch: **`main`**. One branch per user story: `feature/hu-01/create-owner`,
  `feature/hu-02/create-restaurant`, … Bugfixes from a tag: `fix/bug-01/<kebab>`.
- **One HU at a time, with isolated scope. Never mix HUs in one branch.**
- **Before cutting a new HU branch:** update your local `main` from the already-merged previous-HU
  PR, and only then branch.
- Merge each HU branch into `main` when complete; **tag milestones** with annotated semver tags
  (`git tag -a v1.0.0 -m "..."`), then push tags explicitly.
- **Conventional Commits in English**: `feat:`, `fix:`, `test:`, `chore:`, `docs:`, `refactor:`.
- **Never add `Co-Authored-By` or any AI attribution** to commits.

## 7. Working rules

- Work **one HU at a time**; keep the scope of each HU isolated.
- **OpenAPI is mandatory.** Every microservice documents its API with **OpenAPI** (springdoc),
  controllers under `/api/v1/...`. Every HU that adds or changes an endpoint keeps its OpenAPI
  documentation current — it is part of the HU's done-criteria, not optional.
- **Messaging uses AWS SNS.** The SMS + security-PIN flow (HU14, `messaging-service`) is built on
  **AWS SNS** (replacing Twilio). Model the messaging integration behind an SPI port with an SNS
  adapter in `infrastructure`, so the domain never depends on the provider.
- **Order state machine is built at its HU.** The lifecycle in §1 is the reference only; implement
  the state transitions when the owning HU arrives (HU13 / HU15 / HU16 in `food-court-service`),
  not before.
- **HU completion checklist (MANDATORY).** When an HU is finished, before merge/PR, present: the
  HU's name, user-story description, role, and acceptance criteria (verbatim from `docs/reto/`),
  plus a **checklist** mapping each acceptance criterion and each standing rule (custom exceptions
  + validator + runbook, unit tests, OpenAPI docs, hexagonal boundaries, SonarLint clean) to a
  checkable item, so compliance is reviewed together before the HU is closed.
- **SonarLint must pass clean before any PR.** No open SonarLint findings (bugs, vulnerabilities,
  code smells, security hotspots) may remain when a PR is raised — fix them, or suppress with a
  documented justification, first. This is a pre-PR gate alongside the HU completion checklist.
  (SonarLint runs only in the IDE — it cannot be run from the terminal.)
- **ArchUnit + pre-commit hooks are active in all four services** to enforce the hexagonal
  boundaries automatically on every change.
- Keep the hexagonal boundaries clean on every change. When in doubt, the domain stays pure.

## 8. Run locally

```bash
./gradlew clean build        # compile + run tests + jacoco report
./gradlew bootRun            # start the service
```
Requires JDK 21 and a running PostgreSQL (DynamoDB for `traceability-service`). DB connection is
overridable via `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` env vars (see `application.yml`).

- **JWT secret:** `JWT_SECRET` must be **shared and identical across all services** — `users-service`
  signs the token, every other service only verifies it. jjwt selects the HMAC variant from the
  secret's length: the current shared secret resolves to **HS384** (not HS256). To standardise on
  HS256, pin the algorithm explicitly and provide an exact 32-byte secret.
- **CI test profile:** every PostgreSQL service ships an **H2** in-memory test profile so
  `contextLoads()` passes in CI without a live database.

## 9. User-story backlog (shared reference)

**Week 1** — HU1 Create owner *(users)* · HU2 Create restaurant *(food-court)* · HU3 Create dish
*(food-court)* · HU4 Update dish *(food-court)* · HU5 Authentication/JWT *(users)*.
**Week 2** — HU6 Create employee *(users)* · HU7 Enable/disable dish *(food-court)* · HU8 Create
client *(users)* · HU9 List restaurants *(food-court)* · HU10 List dishes *(food-court)* · HU11
Place order *(food-court)* · HU12 List orders by status *(food-court)*.
**Week 3** — HU13 Assign order + IN_PREPARATION *(food-court)* · HU14 Notify ready/SMS+PIN
*(messaging)* · HU15 Deliver order (PIN) *(food-court)* · HU16 Cancel order *(food-court)* · HU17
Order traceability *(traceability)* · HU18 Order efficiency *(traceability)*.

Full acceptance criteria live in `docs/reto/` of the challenge materials.

## 10. Domain error handling & runbook (MANDATORY for every HU)

- **Complete custom exceptions.** Every domain error has its own exception class under
  `domain/exception`, extending `DomainException` — never a bare `DomainException` with an
  ad-hoc string, never a raw `RuntimeException`. Families and their HTTP mapping:
  - validation errors extend `DomainException` → **400**
    (e.g. `RequiredFieldException`, `InvalidEmailFormatException`, `UnderAgeException`);
  - conflicts extend `ConflictException` → **409**
    (e.g. `EmailAlreadyExistsException`, `DocumentNumberAlreadyExistsException`);
  - not-found → `NoDataFoundException` → **404**.
  Each exception carries its own message from `DomainConstants`; `ControllerAdvisor` maps every
  family to its status and returns `{ "message": ... }`.
- **Domain validator.** Each aggregate/HU centralizes its business rules in a domain validator
  (`domain/validation/<Aggregate>Validator`) that throws the custom exceptions. Use cases
  orchestrate and delegate validation to the validator — no inline validation in use cases.
  Validators are pure (no ports) and fully unit-tested.
- **Domain runbook.** Each microservice keeps `docs/DOMAIN-RUNBOOK.md`, updated with every HU:
  for each HU, the business rules, invariants, and a table of custom exception → message →
  HTTP status. It is the living source of truth for the domain's behavior.

## 11. Per-service identity (no archetype references)

The archetype is only a starting guide. Each microservice carries its OWN identity — there must be
**no reference to the base archetype (`powerup`)** anywhere (code, config or docs).
- **Base package:** `com.pragma.<domain>` — `com.pragma.users`, `com.pragma.foodcourt`,
  `com.pragma.messaging`, `com.pragma.traceability`. Never `com.pragma.powerup`.
- **Main class:** `<ServiceName>ServiceApplication` (e.g. `UserServiceApplication`) plus a matching
  `*Tests` class. Never `PowerUpApplication`.
- **Gradle `group`:** `com.pragma`. **`rootProject.name`:** the repo name.
- When bootstrapping a new service from the archetype, strip every `powerup` / `PowerUp` string.
