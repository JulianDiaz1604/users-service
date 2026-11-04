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
- Public: `POST /api/v1/auth/login`, Swagger UI / OpenAPI docs, and `GET /api/v1/users/**`
  (service-to-service owner lookup).
- `POST /api/v1/users/owner` → requires role **ADMINISTRATOR** (from the JWT).
- Any other request requires a valid JWT; unauthenticated access returns **401**
  (`JwtAuthenticationEntryPoint`).

### Exceptions
| Exception | Message | HTTP |
|-----------|---------|------|
| `RequiredFieldException` | All fields are required | 400 |
| `InvalidCredentialsException` | Invalid email or password | 401 |
| (unauthenticated request) | Authentication is required | 401 |

---

## HU6 — Create employee

- **Endpoint:** `POST /api/v1/users/employee`
- **Use case:** `UserUseCase.createEmployee`
- **Validator:** `UserValidator.validateNewEmployee`

### Business rules / invariants
- Only an authenticated **OWNER** may create employees (enforced at `SecurityConfig`:
  `POST /api/v1/users/employee` → `hasRole('OWNER')`).
- Required fields: `name`, `lastName`, `documentNumber`, `phone`, `email`, `password`.
  **`birthDate` is NOT required** for employees (no adult-age rule applies).
- `email` must match a valid email format.
- `phone`: digits only, optional leading `+`, max 13 characters.
- `documentNumber`: digits only.
- `password` is stored **bcrypt-hashed** and never returned in responses.
- The created user is always assigned the **EMPLOYEE** role (`UserRole.EMPLOYEE`, id 3),
  server-side. The `idRol` mentioned in the acceptance criteria is not accepted from the client;
  the role is forced, mirroring HU1 (owner) — a caller cannot elevate the created account.
- `email` and `documentNumber` must be unique.
- Note: `users.birth_date` is nullable to allow employees without a birth date.

### Exceptions
| Exception | Message | HTTP |
|-----------|---------|------|
| `RequiredFieldException` | All fields are required | 400 |
| `InvalidEmailFormatException` | The email format is invalid | 400 |
| `InvalidPhoneFormatException` | The phone must be numeric, may start with '+', and have at most 13 characters | 400 |
| `InvalidDocumentNumberException` | The document number must contain digits only | 400 |
| `EmailAlreadyExistsException` | A user with this email already exists | 409 |
| `DocumentNumberAlreadyExistsException` | A user with this document number already exists | 409 |
| (unauthenticated request) | Authentication is required | 401 |
| (authenticated non-owner) | — | 403 |
