# Domain Runbook — users-service

Living reference for the domain behavior of `users-service`. Updated with **every HU**:
business rules, invariants, and the custom-exception → message → HTTP-status mapping.

## Conventions
- Validation errors extend `DomainException` → **HTTP 400**.
- Conflicts extend `ConflictException` → **HTTP 409**.
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
