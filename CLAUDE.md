# CLAUDE.md — Plazoleta de comidas (PRAGMA PowerUp)

Project conventions for every microservice of the "Plazoleta de comidas" challenge.
These rules are shared across all repositories. The specific microservice this repo
implements is described in `README.md`.

## 1. What we are building

A food-court ordering backend split into **four independent microservices** (one repo each):

| Repo | Responsibility | Datastore |
|------|----------------|-----------|
| `users-service` | Users, roles, authentication (JWT) | PostgreSQL |
| `food-court-service` | Restaurants, dishes, orders | PostgreSQL |
| `messaging-service` | SMS notifications + security PIN (Twilio/SNS) | PostgreSQL |
| `traceability-service` | Order status history & efficiency metrics | MongoDB |

Order lifecycle: `PENDING → IN_PREPARATION → READY → DELIVERED`, plus `PENDING → CANCELLED`.
A client may have only **one active order** at a time.

## 2. Architecture — Hexagonal (ports & adapters). NON-NEGOTIABLE.

Three layers, dependencies point **inward only**: `infrastructure → application → domain`.

- **domain** — pure business logic. No Spring, no JPA, no framework imports. Contains:
  `api` (driving/primary port interfaces), `spi` (driven/secondary port interfaces),
  `model` (domain models), `usecase` (use cases: plain classes implementing `api` ports),
  `exception` (domain exceptions).
- **application** — orchestration. `dto/request`, `dto/response`, `handler` + `handler/impl`
  (`@Service`), `mapper` (MapStruct). May depend on **domain only**.
- **infrastructure** — technical adapters. `configuration` (`BeanConfiguration` wires use cases
  and adapters as `@Bean`), `documentation` (OpenAPI), `exceptionhandler` (`ControllerAdvisor`),
  `input/rest` (controllers), `out/jpa` | `out/mongo` (adapters, entities, mappers, repositories).

Hard rules:
- **domain depends on nothing.** No `org.springframework.*`, no `jakarta.persistence.*` in domain.
- **application must NOT import any `infrastructure` class.**
- Cross-layer wiring is manual in `BeanConfiguration`; only `*Handler` carries `@Service`.
- Swap a datastore or transport without touching domain/application.

## 3. Tech stack (current, stable)

- **Java 21** (LTS, toolchain-pinned) · **Spring Boot 3.5.x** · **Gradle 8.10.x** (wrapper)
- **PostgreSQL** (relational) / **MongoDB** (traceability)
- **MapStruct 1.6.x** (mappers) · **Lombok** (Boot-managed) · **springdoc-openapi 2.x** (Swagger UI)
- **JUnit 5 + Mockito** (tests) · **JaCoCo** (coverage)
- Spring Boot 3 uses the **`jakarta.*`** namespace (never `javax.*`).

## 4. Coding conventions

- **Language: full English** — identifiers, domain model, comments, commits, docs. Domain terms too
  (`Restaurant`, `Dish`, `Order`, `Owner`, `Employee`, `OrderStatus`). No Spanish in code.
- Naming: packages `lowercase`; classes/interfaces `PascalCase`; methods/vars/params `camelCase`;
  constants `UPPER_SNAKE_CASE`; booleans affirmative (`isActive`, `existsOwner`, `hasPin`).
- Interfaces are `I`-prefixed (`IObjectServicePort`, `IObjectPersistencePort`, mappers, repositories).
- Class suffixes: `*Model`, `*UseCase`, `*Handler`, `*RequestDto`, `*ResponseDto`, `*RestController`,
  `*JpaAdapter`, `*Entity`, `*Repository`, `*Mapper`.
- Constructor injection via `@RequiredArgsConstructor`. **No field injection.**
- **No wildcard imports.** SOLID. Short, single-purpose methods. Self-explanatory names over comments.
- MapStruct: `@Mapper(componentModel = "spring", unmappedTargetPolicy = IGNORE)`.
- Passwords hashed with **bcrypt**. Validate per each HU's acceptance criteria (email format,
  phone ≤ 13 chars allowing `+`, numeric document, positive price, adult age, etc.).

## 5. Testing — TDD, mandatory per HU

- Write the test first (red → green → refactor). Every HU ships with unit tests.
- JUnit 5 + Mockito: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`,
  `when(...).thenReturn(...)`, fixtures in `@BeforeEach`.
- **FIRST**: Fast, Independent, Repeatable, Self-validating, Timely. No external deps in unit tests.
- Prioritize use cases and handlers. Name tests after the behavior under test.

## 6. Git workflow

- Integration branch: **`main`**. One branch per user story: `feature/hu-01/create-owner`,
  `feature/hu-02/create-restaurant`, … Bugfixes from a tag: `fix/bug-01/<kebab>`.
- Merge each HU branch into `main` when complete; **tag milestones** with annotated semver tags
  (`git tag -a v1.0.0 -m "..."`), then push tags explicitly.
- **Conventional Commits in English**: `feat:`, `fix:`, `test:`, `chore:`, `docs:`, `refactor:`.
- **Never add `Co-Authored-By` or any AI attribution** to commits.

## 7. Working rules

- Work **one HU at a time**; keep the scope of each HU isolated.
- Every microservice documents its API with **OpenAPI** (springdoc), controllers under `/api/v1/...`.
- Keep the hexagonal boundaries clean on every change. When in doubt, the domain stays pure.

## 8. Run locally

```bash
./gradlew clean build        # compile + run tests + jacoco report
./gradlew bootRun            # start the service
```
Requires JDK 21 and a running PostgreSQL (or MongoDB for traceability). DB connection is
overridable via `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` env vars (see `application.yml`).

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
