# Delta Spec: restaurant-ownership-validation

**Change:** move-employee-creation-and-jwt-identity
**Capability:** restaurant-ownership-validation
**Service:** food-court-service
**Updated:** 2026-11-09

## Context

users-service needs to verify that the calling OWNER actually owns a specific restaurant before
creating an employee there. food-court, which owns restaurant data including the ownerId field,
MUST expose a read endpoint for this cross-service check. The exact URL path and HTTP method are
deferred to design; this spec defines only the observable WHAT.

---

## ADDED Requirements

### REQ-ROV-01: Restaurant Ownership Read Endpoint

food-court MUST expose a read endpoint that, given a restaurantId, returns at least the ownerId
(UUID) of that restaurant. This endpoint exists solely to allow another service to verify ownership
without coupling to food-court's internal domain model.

### REQ-ROV-02: Authentication Requirement

Any caller without a valid JWT MUST be rejected with 401. The endpoint does not restrict by
role beyond requiring a valid JWT (any authenticated role MAY call it).

### REQ-ROV-03: Restaurant Not Found

If the requested restaurantId does not correspond to any existing restaurant, the endpoint
MUST return 404 with the existing message "The restaurant does not exist"
(`RestaurantNotFoundException`).

### REQ-ROV-04: Response Content

The endpoint response MUST include at least `ownerId` (UUID). Additional restaurant fields MAY
be included. The response MUST NOT include sensitive data (e.g. unhashed credentials).

### REQ-ROV-05: Read-Only Semantics

The endpoint MUST NOT modify any state. It is a pure read against existing restaurant data.

### REQ-ROV-06: OpenAPI Documentation

The endpoint MUST be documented in food-court's OpenAPI spec with the response shape and error
responses (401, 404).

---

## Exception Table

| Exception | Message | HTTP |
|-----------|---------|------|
| `RestaurantNotFoundException` | The restaurant does not exist | 404 |
| (unauthenticated request) | Authentication is required | 401 |

---

## Acceptance Scenarios

### Scenario ROV-S01: Existing restaurant returns ownerId

Given restaurant R exists in food-court with ownerId = O
And the caller presents a valid JWT
When the caller queries the ownership endpoint with restaurantId = R
Then the system returns 200 with at least `ownerId = O`.

### Scenario ROV-S02: Non-existent restaurant returns 404

Given no restaurant exists with restaurantId = X
And the caller presents a valid JWT
When the caller queries the ownership endpoint with restaurantId = X
Then the system returns 404 with message "The restaurant does not exist".

### Scenario ROV-S03: Unauthenticated access is rejected

Given the caller presents no JWT or an expired/invalid JWT
When the caller queries the ownership endpoint
Then the system returns 401 with message "Authentication is required".

### Scenario ROV-S04: Endpoint does not mutate state

Given any valid request to the ownership endpoint
When the endpoint handles the request
Then no restaurant, employee, dish, or order record is created, updated, or deleted.

### Scenario ROV-S05: Different restaurants return their respective ownerIds

Given restaurant R1 owned by O1 and restaurant R2 owned by O2 (O1 ≠ O2)
And the caller presents a valid JWT
When the caller queries for R1 and then for R2
Then R1 response has ownerId = O1 and R2 response has ownerId = O2 (no cross-contamination).

### Scenario ROV-S06: Any authenticated role can query ownership

Given an authenticated user with role OWNER, ADMINISTRATOR, or EMPLOYEE and a valid JWT
When the user queries the ownership endpoint for an existing restaurant
Then the system returns 200 with ownerId (no 403 based on role).
