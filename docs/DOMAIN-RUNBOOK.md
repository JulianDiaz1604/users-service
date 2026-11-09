# Domain Runbook — users-service

Living reference for the domain behavior of `users-service`. Updated with **every HU**:
business rules, invariants, and the custom-exception → message → HTTP-status mapping.

## Conventions
- Validation errors extend `DomainException` → **HTTP 400**.
- Conflicts extend `ConflictException` → **HTTP 409**.
- Invalid credentials → `InvalidCredentialsException` (extends `DomainException`) → **HTTP 401**.
- Not found → `NoDataFoundException` → **HTTP 404**.
- Business rules live in `domain/validation/*Validator`; use cases delegate to them.
- `ControllerAdvisor` maps every exception family and returns `{ "message": ... }`.

---

## HU1 — Create owner

- **Endpoint:** `POST /api/v1/users/owner`
- **Use case:** `UserUseCase.createOwner`
- **Validator:** `UserValidator.validateNewOwner`

### Business rules / invariants
- All fields required: `name`, `lastName`, `documentNumber`, `phone`, `birthDate`, `email`, `password`.
- `email` must match a valid email format.
- `phone`: digits only, optional leading `+`, max 13 characters.
- `documentNumber`: digits only.
- The owner must be of legal age (≥ 18 years).
- `password` is stored **bcrypt-hashed** and never returned in responses.
- The created user is assigned the **OWNER** role (`UserRole.OWNER`, id 2).
- `email` and `documentNumber` must be unique.

### Exceptions
| Exception | Message | HTTP |
|-----------|---------|------|
| `RequiredFieldException` | All fields are required | 400 |
| `InvalidEmailFormatException` | The email format is invalid | 400 |
| `InvalidPhoneFormatException` | The phone must be numeric, may start with '+', and have at most 13 characters | 400 |
| `InvalidDocumentNumberException` | The document number must contain digits only | 400 |
| `UnderAgeException` | The owner must be of legal age | 400 |
| `EmailAlreadyExistsException` | A user with this email already exists | 409 |
| `DocumentNumberAlreadyExistsException` | A user with this document number already exists | 409 |

---

## Supporting endpoint for HU2 — Get user by id

- **Endpoint:** `GET /api/v1/users/{id}`
- **Use case:** `UserUseCase.getUserById`
- **Purpose:** consumed by `food-court-service` (HU2) to validate that a restaurant's `ownerId`
  belongs to a user with the **OWNER** role. Returns the user including its `roleId`.

### Business rules / invariants
- Returns the user matching `id`, or 404 when no user exists for that id.
- The response never includes the password.

### Exceptions
| Exception | Message | HTTP |
|-----------|---------|------|
| `NoDataFoundException` | No data found for the requested petition | 404 |

---

## HU5 — Authentication / JWT

- **Endpoint:** `POST /api/v1/auth/login`
- **Use case:** `AuthUseCase.login`
- **Validator:** `AuthValidator.validateLoginRequest`
- **Token port:** `ITokenProviderPort` (driven) → `JwtTokenProviderAdapter` (HS256, shared secret)

### Business rules / invariants
- Login is by **email + password**; both are required.
- The password is verified against the stored **bcrypt** hash via `IPasswordEncoderPort.matches`.
- On success a **JWT** is issued with: `subject` = user id (UUID), `role` claim (role name) and
  `email` claim; signed HS256 with the secret shared across services (`jwt.secret`).
- Login attempts are **unlimited** (no lockout).
- `InvalidCredentialsException` is deliberately generic — it never reveals whether the email or the
  password was the failing part.
- A **bootstrap ADMINISTRATOR** is seeded on startup when absent (`AdminBootstrapConfiguration`),
  because creating owners requires an authenticated admin and no HU creates the admin. Its password
  is never hardcoded — it must be provided via `APP_ADMIN_PASSWORD` (`app.admin.password`); when
  absent, seeding is skipped so no credentials ship in the repository.

### Endpoint authorization (Spring Security, stateless)
- Public: `POST /api/v1/auth/login`, `POST /api/v1/users/client` (client self-registration, HU8),
  Swagger UI / OpenAPI docs, and `GET /api/v1/users/**` (service-to-service owner lookup).
- `POST /api/v1/users/owner` → requires role **ADMINISTRATOR** (from the JWT).
- `POST /api/v1/users/employee` → requires role **OWNER** (from the JWT).
- Any other request requires a valid JWT; unauthenticated access returns **401**
  (`JwtAuthenticationEntryPoint`).

### Exceptions
| Exception | Message | HTTP |
|-----------|---------|------|
| `RequiredFieldException` | All fields are required | 400 |
| `InvalidCredentialsException` | Invalid email or password | 401 |
| (unauthenticated request) | Authentication is required | 401 |

---

## HU6 — Create employee (relocated to users-service, PR 3)

- **Endpoint:** `POST /api/v1/users/employee`
- **Use case:** `UserUseCase.createEmployee(user, restaurantId, requesterOwnerId)`
- **Validator:** `UserValidator.validateNewEmployee`
- **New SPI ports:** `IRestaurantOwnershipPort`, `IEmployeeRestaurantLinkPort`
- **Adapter:** `FoodCourtRestClientAdapter` (implements both ports over RestClient)

### Business rules / invariants
- Only an authenticated **OWNER** may create employees (enforced at `SecurityConfig`:
  `POST /api/v1/users/employee` → `hasRole('OWNER')`).
- Request body requires: `name`, `lastName`, `documentNumber`, `phone`, `email`, `password`,
  **`restaurantId`** (UUID). **`birthDate` is NOT required** (no adult-age rule applies).
- `email` must match a valid email format.
- `phone`: digits only, optional leading `+`, max 13 characters.
- `documentNumber`: digits only.
- `password` is stored **bcrypt-hashed** and never returned in responses.
- The created user is always assigned the **EMPLOYEE** role (server-side, never from the caller).
- `email` and `documentNumber` must be unique.
- Note: `users.birth_date` is nullable to allow employees without a birth date.

### Orchestration (design §Data Flow — PR 3)
1. Validate employee fields (`UserValidator.validateNewEmployee`).
2. `GET /api/v1/restaurants/{restaurantId}/owner` via food-court (fail fast — no side effects).
   - 404 → `RestaurantNotFoundException` (HTTP 404).
   - 200 with null `ownerId` → `OwnerNotFoundException` (HTTP 404).
   - Unreachable (connection refused/timeout) or unexpected status → `RestaurantServiceUnavailableException` (HTTP 502).
3. Compare returned `ownerId` to `requesterOwnerId` (JWT subject).
   - Mismatch → `NotRestaurantOwnerException` (HTTP 403).
4. Persist account (bcrypt + uniqueness checks → 409 on duplicate).
5. `POST /api/v1/restaurants/{restaurantId}/employees` with `{"employeeId":"<uuid>"}` (food-court).
   - 409 → `EmployeeAlreadyExistsException` (HTTP 409) — no compensation.
   - Other failure → compensating delete of the just-created user → `EmployeeLinkRegistrationException` (HTTP 502).
6. Return created employee (password excluded).

### Authorization header forwarding
The inbound owner JWT is propagated to food-court via `AuthorizationForwardingInterceptor`
(registered on `foodCourtRestClient`). food-court re-validates ownership on the POST (defense-in-depth).

### New configuration property
`foodcourt.base-url` (default `http://localhost:8080`; override via `FOODCOURT_BASE_URL`).

### Exceptions
| Exception | Message | HTTP |
|-----------|---------|------|
| `RequiredFieldException` | All fields are required | 400 |
| `InvalidEmailFormatException` | The email format is invalid | 400 |
| `InvalidPhoneFormatException` | The phone must be numeric, may start with '+', and have at most 13 characters | 400 |
| `InvalidDocumentNumberException` | The document number must contain digits only | 400 |
| `RestaurantNotFoundException` | The restaurant does not exist | 404 |
| `OwnerNotFoundException` | The restaurant has no registered owner | 404 |
| `NotRestaurantOwnerException` | This action can only be performed by the owner of the restaurant | 403 |
| `RestaurantServiceUnavailableException` | The restaurant service is unavailable; please try again later | 502 |
| `EmailAlreadyExistsException` | A user with this email already exists | 409 |
| `DocumentNumberAlreadyExistsException` | A user with this document number already exists | 409 |
| `EmployeeAlreadyExistsException` | The employee is already registered for this restaurant | 409 |
| `EmployeeLinkRegistrationException` | Failed to register the employee-restaurant link; account was rolled back | 502 |
| (unauthenticated request) | Authentication is required | 401 |
| (authenticated non-owner role) | — | 403 (Spring Security) |

---

## HU8 — Create client

- **Endpoint:** `POST /api/v1/users/client`
- **Use case:** `UserUseCase.createClient`
- **Validator:** `UserValidator.validateNewClient`

### Business rules / invariants
- **Public self-registration:** anyone may create a client account (permitted at `SecurityConfig`:
  `POST /api/v1/users/client` → `permitAll`). No authentication is required, because a client
  registers themselves to access the system and place orders.
- Required fields: `name`, `lastName`, `documentNumber`, `phone`, `email`, `password`.
  **`birthDate` is NOT required** for clients (no adult-age rule applies).
- `email` must match a valid email format.
- `phone`: digits only, optional leading `+`, max 13 characters.
- `documentNumber`: digits only.
- `password` is stored **bcrypt-hashed** and never returned in responses.
- The created user is always assigned the **CLIENT** role (`UserRole.CLIENT`, id 4), server-side.
  The `idRol` mentioned in the acceptance criteria is not accepted from the client; the role is
  forced, mirroring HU1/HU6 — a caller cannot self-assign a privileged role.
- `email` and `documentNumber` must be unique.

### Exceptions
| Exception | Message | HTTP |
|-----------|---------|------|
| `RequiredFieldException` | All fields are required | 400 |
| `InvalidEmailFormatException` | The email format is invalid | 400 |
| `InvalidPhoneFormatException` | The phone must be numeric, may start with '+', and have at most 13 characters | 400 |
| `InvalidDocumentNumberException` | The document number must contain digits only | 400 |
| `EmailAlreadyExistsException` | A user with this email already exists | 409 |
| `DocumentNumberAlreadyExistsException` | A user with this document number already exists | 409 |
