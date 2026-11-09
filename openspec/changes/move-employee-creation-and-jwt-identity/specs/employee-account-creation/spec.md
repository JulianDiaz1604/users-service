# Delta Spec: employee-account-creation

**Change:** move-employee-creation-and-jwt-identity
**Capability:** employee-account-creation
**Service:** users-service
**Updated:** 2026-11-09

## Context

HU6 employee creation moves from food-court (which orchestrated the call) to users-service as the
authoritative HTTP entry point. The caller's identity (ownerId) comes from the JWT subject claim; the
endpoint validates that the caller owns the specified restaurant via food-court's
restaurant-ownership-validation endpoint before creating the employee account and registering the
employee-restaurant link.

---

## ADDED Requirements

### REQ-EAC-01: Endpoint Access Control

users-service MUST expose `POST /api/v1/users/employee` restricted to callers holding the
`OWNER` role in their JWT. Unauthenticated requests MUST be rejected with 401. Authenticated
callers with any role other than OWNER MUST be rejected with 403.

### REQ-EAC-02: JWT-Derived Owner Identity

The creating OWNER's identity (userId) MUST be read from the JWT subject claim. The request
body MUST NOT carry an ownerId or roleId field. Any role or roleId value in the request body
MUST be ignored — the server forces EMPLOYEE regardless.

### REQ-EAC-03: Required Request Fields

The following fields are REQUIRED in the request body. Any missing or blank field MUST be
rejected with 400 (`RequiredFieldException`):

- `name`
- `lastName`
- `documentNumber`
- `phone`
- `email`
- `password`
- `restaurantId` (UUID of the restaurant where the employee will work)

`birthDate` is NOT required and NOT validated (no adult-age rule applies to employees).

### REQ-EAC-04: Field Format Validation

- `email` MUST match a valid email format; otherwise 400 (`InvalidEmailFormatException`).
- `phone` MUST consist of digits only with an optional leading `+` and MUST be at most 13 characters;
  otherwise 400 (`InvalidPhoneFormatException`).
- `documentNumber` MUST contain digits only; otherwise 400 (`InvalidDocumentNumberException`).

### REQ-EAC-05: Restaurant Ownership Validation

Before creating the employee account, users-service MUST verify that the calling OWNER owns the
specified `restaurantId`. This verification is performed via food-court's
restaurant-ownership-validation endpoint (see `restaurant-ownership-validation` spec). If the
restaurant does not exist in food-court, the request MUST be rejected with 404
(`RestaurantNotFoundException`). If the restaurant exists but the calling OWNER does not own it
(JWT userId ≠ restaurant.ownerId), the request MUST be rejected with 403
(`NotRestaurantOwnerException`).

### REQ-EAC-06: Forced EMPLOYEE Role

The created user MUST always receive the `EMPLOYEE` role (server-assigned). The role is never
taken from the request body. No request field can override this assignment.

### REQ-EAC-07: Uniqueness Constraints

`email` and `documentNumber` MUST be unique across all users. Violation MUST be rejected with 409:

- Duplicate `email` → `EmailAlreadyExistsException`.
- Duplicate `documentNumber` → `DocumentNumberAlreadyExistsException`.

### REQ-EAC-08: Password Handling

The employee's `password` MUST be hashed with bcrypt before persistence. The hashed password
MUST NOT appear in any response body.

### REQ-EAC-09: Employee-Restaurant Link Registration

After the user account is created in users-service, users-service MUST trigger the registration of
the employee-restaurant link in food-court (employeeId, restaurantId). This link is persisted in
food-court's data store, not in users-service.

### REQ-EAC-10: Response Shape

On success (201 Created), the response MUST include the new user's `id` (UUID) and personal data
(`name`, `lastName`, `email`, `documentNumber`, `phone`). The `password` field MUST be excluded
from the response.

### REQ-EAC-11: OpenAPI Documentation

The endpoint MUST be documented in users-service's OpenAPI spec with request and response shapes,
required fields, and error responses (400, 401, 403, 404, 409).

---

## Exception Table

| Exception | Message | HTTP |
|-----------|---------|------|
| `RequiredFieldException` | All fields are required | 400 |
| `InvalidEmailFormatException` | The email format is invalid | 400 |
| `InvalidPhoneFormatException` | The phone must be numeric, may start with '+', and have at most 13 characters | 400 |
| `InvalidDocumentNumberException` | The document number must contain digits only | 400 |
| `RestaurantNotFoundException` | The restaurant does not exist | 404 |
| `NotRestaurantOwnerException` | This action can only be performed by the owner of the restaurant | 403 |
| `EmailAlreadyExistsException` | A user with this email already exists | 409 |
| `DocumentNumberAlreadyExistsException` | A user with this document number already exists | 409 |
| (unauthenticated request) | Authentication is required | 401 |
| (authenticated non-owner) | — | 403 |

---

## Acceptance Scenarios

### Scenario EAC-S01: Successful employee creation

Given an authenticated OWNER whose JWT userId matches the ownerId of restaurant R
When the OWNER sends `POST /api/v1/users/employee` with valid fields (name, lastName,
  documentNumber, phone, email, password, restaurantId=R)
Then the system creates the user with role EMPLOYEE, bcrypt-hashes the password, persists the
  account, registers the employee-restaurant link in food-court, and returns 201 with the new
  userId and personal data (excluding password).

### Scenario EAC-S02: Missing required field

Given an authenticated OWNER
When the OWNER sends `POST /api/v1/users/employee` with any required field absent or blank
Then the system returns 400 with message "All fields are required".

### Scenario EAC-S03: Invalid email format

Given an authenticated OWNER
When the OWNER sends a request with an email that does not match a valid email format
  (e.g. "not-an-email")
Then the system returns 400 with message "The email format is invalid".

### Scenario EAC-S04: Invalid phone format

Given an authenticated OWNER
When the OWNER sends a request with a phone value that contains non-digit characters
  (excluding an optional leading '+') or exceeds 13 characters
Then the system returns 400 with message
  "The phone must be numeric, may start with '+', and have at most 13 characters".

### Scenario EAC-S05: Invalid document number

Given an authenticated OWNER
When the OWNER sends a request with a documentNumber that contains non-digit characters
Then the system returns 400 with message "The document number must contain digits only".

### Scenario EAC-S06: Restaurant not found

Given an authenticated OWNER
When the OWNER sends a request with a restaurantId that does not exist in food-court
Then the system returns 404 with message "The restaurant does not exist".

### Scenario EAC-S07: Caller does not own the restaurant

Given authenticated OWNER O1 and restaurant R owned by OWNER O2 (O1 ≠ O2)
When O1 sends `POST /api/v1/users/employee` with restaurantId=R
Then food-court's ownership check reveals O1.userId ≠ R.ownerId and the system returns 403
  with message "This action can only be performed by the owner of the restaurant".

### Scenario EAC-S08: Duplicate email

Given an authenticated OWNER who owns the target restaurant
When the OWNER sends a request with an email that already exists for another user
Then the system returns 409 with message "A user with this email already exists".

### Scenario EAC-S09: Duplicate document number

Given an authenticated OWNER who owns the target restaurant
When the OWNER sends a request with a documentNumber that already exists for another user
Then the system returns 409 with message "A user with this document number already exists".

### Scenario EAC-S10: Unauthenticated request

Given a request with no JWT or an expired/invalid JWT
When the request hits `POST /api/v1/users/employee`
Then the system returns 401 with message "Authentication is required".

### Scenario EAC-S11: Authenticated but wrong role

Given an authenticated user whose JWT role is CLIENT, EMPLOYEE, or ADMINISTRATOR
When the user sends `POST /api/v1/users/employee`
Then Spring Security returns 403.

### Scenario EAC-S12: Role cannot be overridden by caller

Given an authenticated OWNER who owns the target restaurant and provides all valid fields
When the request body includes any role or roleId field set to a non-EMPLOYEE value
Then the system ignores it and creates the user with role EMPLOYEE.

### Scenario EAC-S13: No birthDate validation for employees

Given an authenticated OWNER who owns the target restaurant
When the OWNER sends a request that omits birthDate entirely
Then the system accepts the request and does not reject it for a missing or under-age birthDate.
