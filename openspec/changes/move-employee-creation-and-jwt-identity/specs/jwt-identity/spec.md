# Delta Spec: jwt-identity

**Change:** move-employee-creation-and-jwt-identity
**Capability:** jwt-identity
**Service:** users-service and food-court-service (cross-cutting)
**Updated:** 2026-11-09

## Context

Both services currently identify the authenticated caller via a raw String principal (the JWT
subject / userId). This change standardises caller identity into a typed authenticated principal
carrying userId, role, and optionally restaurantId. It also eliminates food-court's outbound
`isOwner` REST call to users-service (the circular dependency) by trusting the JWT role claim
instead, and optionally extends the JWT with a restaurantId claim. All existing
`@AuthenticationPrincipal` usages in both services MUST keep working after migration.

---

## ADDED Requirements

### REQ-JI-01: Typed Authenticated Principal

Both services MUST use a typed, structured principal type (not a raw String) to represent the
authenticated caller. The principal MUST expose at minimum:

- `userId` (UUID) — from the JWT subject claim.
- `role` (String) — from the JWT role claim.
- `restaurantId` (UUID, nullable) — from the JWT restaurantId claim when present; null otherwise.

### REQ-JI-02: Optional restaurantId Claim in JWT

The JWT issued by users-service MAY include a `restaurantId` claim. The conditions under which
this claim is populated are deferred to design. When present, the claim MUST carry a valid UUID.
When absent, the typed principal MUST expose `restaurantId = null` without error.

### REQ-JI-03: Existing Controllers Keep Working After Migration

All existing `@AuthenticationPrincipal` usages in both services MUST keep working after the
typed principal is introduced. No existing endpoint behavior or business rule MUST change as
a consequence of adopting the typed principal — the type changes, the semantics do not.

### REQ-JI-04: Elimination of Outbound Role Validation from food-court

food-court MUST NOT make outbound REST calls to users-service to verify user roles. The
`IUserValidationPort` SPI and its `UserValidationRestClientAdapter` implementation MUST be
removed. Authorization decisions in food-court that depended on this call MUST instead use the
JWT role claim (via Spring Security's claim-based role evaluation).

### REQ-JI-05: Claim-Based Authorization Covers All Secured Endpoints

food-court's Spring Security configuration MUST enforce role-based access solely using JWT role
claims, with no outbound user lookup. This MUST cover all endpoints currently gated on a specific
role (ADMINISTRATOR, OWNER, EMPLOYEE, CLIENT).

### REQ-JI-06: JWT Algorithm and Secret Unchanged

The JWT signing algorithm (HS256) and the shared secret (`jwt.secret`) MUST remain unchanged.
The optional `restaurantId` claim extends the existing payload without breaking backward
compatibility.

### REQ-JI-07: Removal of AuthorizationForwardingInterceptor

food-court's `AuthorizationForwardingInterceptor` (used to forward the OWNER's JWT to
users-service when creating an employee through the old path) MUST be removed, provided it is not
used by any other remaining outbound call.

### REQ-JI-08: JWT Backward Compatibility

Tokens issued before the restaurantId claim was added (i.e. tokens with no restaurantId claim)
MUST be accepted and parsed without error. The typed principal MUST expose `restaurantId = null`
for such tokens.

---

## Exception Table

| Condition | HTTP |
|-----------|------|
| Unauthenticated request to any secured endpoint | 401 |
| Authenticated but wrong role for the endpoint | 403 |

---

## Acceptance Scenarios

### Scenario JI-S01: Typed principal with userId, role, and restaurantId

Given a request with a valid JWT carrying subject = U, role = OWNER, restaurantId = R
When the security filter processes the request in either service
Then the authenticated principal exposes userId = U, role = "OWNER", restaurantId = R.

### Scenario JI-S02: Typed principal with no restaurantId claim

Given a request with a valid JWT carrying subject = U, role = CLIENT, and no restaurantId claim
When the security filter processes the request
Then the authenticated principal exposes userId = U, role = "CLIENT", restaurantId = null
  (no parsing error).

### Scenario JI-S03: Existing food-court OWNER endpoints keep working after migration

Given an authenticated OWNER with a valid JWT
When the OWNER calls `POST /api/v1/dishes` (or any endpoint that reads ownerId from principal)
Then the endpoint reads ownerId from the typed principal's userId field and applies the existing
  ownership rule — no behavior change from before the migration.

### Scenario JI-S04: Existing food-court EMPLOYEE endpoint keeps working after migration

Given an authenticated EMPLOYEE with a valid JWT
When the EMPLOYEE calls `GET /api/v1/orders` (or any endpoint that reads employeeId from principal)
Then the endpoint reads employeeId from the typed principal's userId field — no behavior change.

### Scenario JI-S05: food-court does not call users-service for role validation

Given any secured request to food-court
When food-court authorizes the request
Then no outbound HTTP call to users-service's `GET /api/v1/users/{id}` is made for role
  verification. Authorization is decided solely from JWT claims.

### Scenario JI-S06: food-court ADMINISTRATOR endpoint rejects OWNER

Given an authenticated OWNER
When the OWNER calls `POST /api/v1/restaurants`
Then Spring Security rejects the request with 403 (role ADMINISTRATOR required).

### Scenario JI-S07: food-court OWNER endpoint rejects CLIENT

Given an authenticated CLIENT
When the CLIENT calls `POST /api/v1/dishes`
Then Spring Security rejects the request with 403 (role OWNER required).

### Scenario JI-S08: restaurantId claim is a valid UUID when present

Given a JWT containing a restaurantId claim with value R (UUID string)
When the security filter parses the token
Then the typed principal's restaurantId equals UUID.fromString(R) without error.

### Scenario JI-S09: JWT without restaurantId claim is accepted by both services

Given a JWT that carries no restaurantId claim (e.g. issued before this change)
When either service's security filter processes the token
Then the token is accepted and the principal's restaurantId is null.

### Scenario JI-S10: Both services reject expired or tampered JWT

Given a request carrying a JWT with an invalid signature or past expiry
When either service's security filter processes the token
Then the request is rejected with 401 with message "Authentication is required".
