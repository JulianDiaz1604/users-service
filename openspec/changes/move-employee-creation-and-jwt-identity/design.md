# Design: Relocate Employee Creation to users-service and Standardize JWT Identity

## Technical Approach

users-service becomes the HTTP entry for employee creation. Its `UserUseCase` orchestrates
through two new domain SPI ports — ownership read + link registration — implemented by a single
RestClient adapter calling food-court. food-court exposes an ownership GET and repurposes its
employee POST to register the link only; it drops both outbound calls to users-service, so the
sole direction is `users-service → food-court`. Both services gain a typed `AuthenticatedUser`
principal in infrastructure. Realizes proposal capabilities `employee-account-creation`,
`restaurant-ownership-validation`, `jwt-identity`.

## Architecture Decisions

### Decision: Link persistence mechanism + ordering
**Choice**: Synchronous RestClient. Order: (1) validate fields, (2) GET ownership (fail fast, no
side effect), (3) persist account → `employeeId`, (4) POST link to food-court. On step-4 failure,
best-effort compensating delete of the just-created user, then surface 502.
**Alternatives**: link-first (impossible — needs `employeeId`); async event/outbox (over-engineered,
no messaging infra in scope); leave orchestration in food-court (recreates the cycle).
**Rationale**: GET-first removes orphan accounts on the common not-owner/404 path; matches the
existing synchronous RestClient style; residual orphan window only on rare transient link failure,
mitigated by compensation.

### Decision: Inter-service contracts + auth
**Choice**:
- `GET /api/v1/restaurants/{restaurantId}/owner` → `200 {"restaurantId","ownerId"}`; `404` when missing. Authenticated.
- `POST /api/v1/restaurants/{restaurantId}/employees` → body `{"employeeId":"<uuid>"}` → `201`; `409` on duplicate (`employee_id` unique). `hasRole(OWNER)`, and food-court **re-validates** that the JWT `sub` owns `restaurantId` (defense in depth).
- Auth = **forwarded owner JWT** (users-service propagates inbound `Authorization` to food-court via a request interceptor).
**Alternatives**: service-to-service credential (adds secret mgmt, loses per-owner identity needed
for the defense-in-depth re-check); trust users-service without food-court re-validation (weaker).
**Rationale**: shared HS256 secret already exists; no new infra; food-court owns restaurant data so
it is the correct place to re-assert ownership on write.

### Decision: `restaurantId` token claim
**Choice**: NOT in OWNER tokens — owners hold 0..n restaurants created post-login; employee-creation
ownership uses the owner's `sub` + food-court lookup, never a claim. Claim belongs **only** to
EMPLOYEE tokens (one restaurant each), set by users-service token generation, read by food-court's
typed principal for HU12/HU13. food-court keeps local `findRestaurantIdByEmployee` as fallback, so
HU12/HU13 keep working whether or not the claim is populated.
**Alternatives**: owned-restaurantId in OWNER token (fragile/stale).
**Rationale**: confirms the claim is an EMPLOYEE optimization, explicitly NOT the ownership
mechanism; backward-compatible via fallback.

### Decision: Typed principal placement
**Choice**: `record AuthenticatedUser(UUID userId, String role, UUID restaurantId)` in each service's
`infrastructure/configuration/security`. The JWT filter sets it as the `Authentication` principal
(authorities still `ROLE_<role>`). Controllers use `@AuthenticationPrincipal AuthenticatedUser`.
**Rationale**: principal is a security/transport concern — keeping it out of domain preserves purity.

### Decision: Remove food-court `IUserValidationPort` (HU2)
**Choice**: Drop `IUserValidationPort`/`UserValidationRestClientAdapter` and the `isOwner` cross-call.
`ownerId` **stays required in `CreateRestaurantRequestDto`** (an ADMIN token's `sub` is the admin, not
the owner). `RestaurantValidator` gains an `ownerId`-present check; cross-service owner-role
verification is intentionally dropped to break the cycle (ADMIN endpoint is role-gated via the claim).
**Rationale**: removing the outbound call is required for the acyclic direction; HU2 stays functional.

## Data Flow

    Owner(JWT) ─POST /api/v1/users/employee─▶ users UserRestController
                                               │ UserUseCase.createEmployee(user,restaurantId,sub)
                                               │ 1 validate fields
                  GET /restaurants/{id}/owner ◀┤ 2 ownership (forward JWT) → 404/403
      food RestaurantController ──ownerId──────▶│   compare ownerId==sub
                                               │ 3 save account ⇒ employeeId
             POST /restaurants/{id}/employees ◀┤ 4 register link (forward JWT); 409 dup
      food EmployeeController ──201────────────▶│   re-validate ownership (defense-in-depth)
                                               │ 5 link fail ⇒ compensating delete ⇒ 502
                                               ▼ 201 UserResponse

## File Changes

| File | Action | Description |
|------|--------|-------------|
| users `domain/spi/IRestaurantOwnershipPort`, `IEmployeeRestaurantLinkPort` | Create | `getOwnerId`, `registerEmployee` |
| users `domain/usecase/UserUseCase` | Modify | `createEmployee(user,restaurantId,ownerId)` orchestration + compensation |
| users `domain/exception/*` | Create | `RestaurantNotFound`(404),`NotRestaurantOwner`(403),`EmployeeLinkRegistration`(502) |
| users `application/dto/request/CreateEmployeeRequestDto` | Modify | add `restaurantId` |
| users `input/rest/UserRestController` + `UserHandler` | Modify | `@AuthenticationPrincipal AuthenticatedUser`, pass restaurantId |
| users `out/client/FoodCourtRestClientAdapter` + `AuthorizationForwardingInterceptor` | Create | both SPI ports; forward owner JWT |
| users `security/AuthenticatedUser`, `JwtAuthenticationFilter`, `JwtTokenProviderAdapter` | Create/Modify | typed principal; optional `restaurantId` claim |
| users `configuration/BeanConfiguration` | Modify | wire ports/adapter/`foodCourtRestClient`/base-url |
| food `input/rest/RestaurantRestController` | Modify | add `GET /{restaurantId}/owner` |
| food `input/rest/EmployeeRestController` | Modify | POST registers link only (`{employeeId}`) |
| food `domain/usecase/EmployeeUseCase` | Modify | `registerEmployee`; drop `IUserCreationPort` |
| food `domain/usecase/RestaurantUseCase` + `api` + `RestaurantValidator` | Modify | `findOwnerId`; drop `IUserValidationPort`; ownerId-present check |
| food `IUserValidationPort`,`UserValidationRestClientAdapter`,`IUserCreationPort`,`UserCreationRestClientAdapter`,`AuthorizationForwardingInterceptor`,`UserClientResponse`,`CreateEmployeeClientRequest`,`usersRestClient` bean | Delete | remove reverse calls |
| food `security/AuthenticatedUser`,`JwtAuthenticationFilter`,`JwtTokenValidator` | Create/Modify | typed principal + `restaurantId`; migrate 7 `@AuthenticationPrincipal String` (Dish×3, Order×3, Employee×1) |

## Interfaces / Contracts

```java
public record AuthenticatedUser(java.util.UUID userId, String role, java.util.UUID restaurantId) {}
// users SPI
UUID getOwnerId(UUID restaurantId);              // null/404 ⇒ RestaurantNotFoundException
void registerEmployee(UUID restaurantId, UUID employeeId); // 409 ⇒ EmployeeAlreadyExists
```

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | `UserUseCase.createEmployee`: owner ok / not-owner(403) / missing(404) / link-fail→compensating delete | Mockito on SPI ports |
| Unit | food `RestaurantUseCase.findOwnerId`, `EmployeeUseCase.registerEmployee` (ownership+409), `RestaurantValidator` ownerId-present | pure |
| Unit | `JwtAuthenticationFilter` builds `AuthenticatedUser` with/without `restaurantId` | mock filter chain |
| Adapter | `FoodCourtRestClientAdapter` maps 404→NotFound, 409→AlreadyExists, forwards Authorization | `MockRestServiceServer` / mocked `RestClient` |

Strict TDD: RED tests first for every use case/validator/adapter above.

## Threat Matrix

N/A — no shell, subprocess, VCS/PR automation, or executable-file classification boundary. The
matrix rows (git/commit/push/PR/doc-exec) do not apply. The relevant risk is JWT credential
forwarding: pin `foodCourtRestClient` to the configured base-url only, forward `Authorization`
solely to food-court, and have food-court re-validate ownership — covered by the adapter and
defense-in-depth tests above.

## Migration / Rollout

Per-repo PRs. Slice order: (1) typed-principal foundation in both services (additive; migrate 7
usages) → (2) food-court ownership GET + link-only POST + removals → (3) users-service relocation
(new ports/adapter/endpoint) → (4) delete food-court reverse adapters once users path verified.
No data migration (`employee_restaurant` unchanged). **Requires food-court edit authority**
(`allowedEditRoots` currently `[users-service]`).

## Open Questions

- [ ] Populate the EMPLOYEE `restaurantId` claim at login now (adds a users→food-court read) or defer
  and rely on food-court's local fallback? Design defaults to the fallback; claim slot is introduced.
