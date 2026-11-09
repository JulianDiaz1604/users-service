# Proposal: Relocate Employee Creation to users-service and Standardize JWT Identity

## Intent

HU6 employee creation is currently orchestrated by food-court (`EmployeeRestController` → `UserCreationRestClientAdapter` → users-service), while users-service already fully implements account creation (`UserUseCase.createEmployee`). This splits ownership awkwardly and, combined with food-court's HU2 outbound `isOwner` call to users-service, risks a circular dependency. User-approved decision: make users-service the HTTP entry point for employee creation, validate ownership against food-court (owner of restaurant data) using JWT claims, and remove food-court's reverse call. Also standardize caller identity (today a raw String principal) into a typed, reusable pattern.

## Scope

### In Scope
- Move the employee-creation HTTP entry point into users-service; it reads owner identity from JWT and creates the account.
- users-service validates restaurant ownership against a new food-court read endpoint.
- users-service ensures the employee↔restaurant link is persisted in food-court (link stays in food-court).
- Remove food-court's old employee endpoint, `UserCreationRestClientAdapter`, and `AuthorizationForwardingInterceptor` (if unused).
- Remove food-court HU2 outbound `isOwner` call; read `role` from the JWT claim instead.
- Introduce a typed authenticated principal across both secured services; migrate the 7 food-court `@AuthenticationPrincipal` usages; add `restaurantId` claim where relevant.

### Out of Scope
- Moving the `employee_restaurant` table (HU12/HU13 depend on it in food-court).
- Adding security to traceability/messaging (document the pattern only).
- Changing JWT algorithm/secret or token issuance ownership (stays users-service).

## Capabilities

### New Capabilities
- `employee-account-creation`: users-service HTTP entry, JWT-derived owner identity, ownership validation, account creation, link registration.
- `restaurant-ownership-validation`: food-court read endpoint exposing restaurant ownership for cross-service checks.
- `jwt-identity`: typed principal (userId + role + optional restaurantId), `restaurantId` claim, claim-based role checks replacing HU2 outbound call.

### Modified Capabilities
- None (no pre-existing specs in `openspec/specs/`).

## Approach

End-state dependency direction: `users-service → food-court` only. users-service becomes the employee-creation entry; food-court exposes an ownership read endpoint and drops its outbound user-validation call by trusting the JWT `role` claim. A shared typed principal replaces the raw String principal. Exact contract shapes and link-persistence mechanism are left to sdd-design.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `users-service/.../input/rest` + `UserUseCase` | New/Modified | New employee-creation entry; JWT identity; calls food-court |
| `food-court-service/.../EmployeeRestController` + `UserCreationRestClientAdapter` + `AuthorizationForwardingInterceptor` | Removed | Old orchestration path |
| `food-court-service` restaurant REST | New | Ownership read endpoint |
| `food-court` HU2 `IUserValidationPort`/`UserValidationRestClientAdapter` | Removed | Replaced by JWT `role` claim |
| Both services' security/principal | Modified | Typed principal; `restaurantId` claim; 7 `@AuthenticationPrincipal` migrations |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Edit authority limited to users-service (`allowedEditRoots`) | High | Grant food-court edit authority via consent flow, or slice per repo |
| Principal migration breaks 7 food-court controllers | Med | Typed principal + tests before removal |
| `restaurantId` claim fragile for multi-restaurant owners | Med | Defer claim-vs-contextual decision to design |
| CI `contextLoads()` without DB | Med | H2 test profile per Postgres service |

## Rollback Plan

Changes land per-repo behind PRs. Revert the users-service PR to restore food-court orchestration; keep food-court's old endpoint/adapter until the users-service path is verified in each slice, then remove.

## Dependencies

- Cross-repo edit authority for food-court-service.
- Shared JWT secret/algorithm (jjwt 0.12.6, HS256) unchanged.

## Success Criteria

- [ ] Employee creation succeeds via users-service entry using JWT identity.
- [ ] Ownership validated against food-court; link persisted in food-court.
- [ ] food-court no longer calls users-service (no circular dependency).
- [ ] Typed principal in both services; 7 `@AuthenticationPrincipal` usages migrated.
- [ ] OpenAPI updated; SonarLint clean; CI green with H2 profile.
