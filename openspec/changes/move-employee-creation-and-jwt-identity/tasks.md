# Tasks: Move Employee Creation and Standardize JWT Identity

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1,000–1,100 (both repos) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (foundation, both) → PR 2 (food-court endpoints) → PR 3 (users relocation) → PR 4 (removals) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Work Units

| Unit | Goal | PR | Test command | Harness | Rollback |
|------|------|----|-------------|---------|----------|
| 1 | Typed principal foundation — both services | PR 1 | `./gradlew test` (both repos) | N/A unit-only | Revert filter + record files |
| 2 | food-court ownership GET + link-only POST | PR 2 | `./gradlew test` food-court | N/A unit-only | Revert controller + use case |
| 3 | users-service employee creation relocation | PR 3 | `./gradlew test` users | N/A unit-only | Revert use case + adapter |
| 4 | food-court reverse adapter removals | PR 4 | `./gradlew test` food-court | N/A unit-only | Restore deleted files via git |

> **Edit authority required** for `/Users/juliandiaz/Projects/PRAGMA/Reto1/food-court-service` before applying any `[fc]` task (currently `allowedEditRoots = [users-service]`).

---

## Slice 1: Typed AuthenticatedUser Foundation — Both Services [PR 1]

- [x] 1.1 [users] Create `src/main/java/com/pragma/users/infrastructure/configuration/security/AuthenticatedUser.java` record (`userId UUID`, `role String`, `restaurantId UUID`).
- [x] 1.2 [users] RED — `JwtAuthenticationFilterTest`: builds typed `AuthenticatedUser` with `restaurantId` claim (JI-S01, JI-S08) and without it → null (JI-S02, JI-S09).
- [x] 1.3 [users] GREEN — Modify `JwtAuthenticationFilter` to set `AuthenticatedUser` as principal; `JwtTokenProviderAdapter` emits optional `restaurantId` claim for EMPLOYEE tokens.
- [x] 1.4 [fc] Create `/Users/juliandiaz/Projects/PRAGMA/Reto1/food-court-service/src/main/java/com/pragma/foodcourt/infrastructure/configuration/security/AuthenticatedUser.java` (same record shape).
- [x] 1.5 [fc] RED — food-court `JwtAuthenticationFilterTest`: typed principal with/without `restaurantId` (JI-S01, JI-S02, JI-S08–S10).
- [x] 1.6 [fc] GREEN — Modify `/Users/juliandiaz/Projects/PRAGMA/Reto1/food-court-service/src/main/java/com/pragma/foodcourt/infrastructure/configuration/security/JwtAuthenticationFilter.java` + `JwtTokenValidator.java` to set typed `AuthenticatedUser`.
- [x] 1.7 [fc] Migrate 6 `@AuthenticationPrincipal String` → `@AuthenticationPrincipal AuthenticatedUser` in `/Users/juliandiaz/Projects/PRAGMA/Reto1/food-court-service/src/main/java/com/pragma/foodcourt/infrastructure/input/rest/` (DishRestController×3, OrderRestController×2, EmployeeRestController×1) (JI-S03, JI-S04). Note: tasks.md listed OrderRestController×3 but on-disk after HU13 reset only 2 `@AuthenticationPrincipal` usages exist — both migrated.

## Slice 2: food-court Ownership GET + Link-Only POST [PR 2]

- [x] 2.1 [fc] RED — `RestaurantUseCaseTest.findOwnerId`: returns ownerId for existing; throws `RestaurantNotFoundException` for missing (ROV-S01, ROV-S02, ROV-S05).
- [x] 2.2 [fc] GREEN — Add `findOwnerId(UUID restaurantId)` to `IRestaurantServicePort` api and `RestaurantUseCase`; add `OwnerIdResponseDto`.
- [x] 2.3 [fc] RED — `RestaurantRestControllerTest`: 200+ownerId, 404, 401 for `GET /api/v1/restaurants/{restaurantId}/owner` (ROV-S01–S06).
- [x] 2.4 [fc] GREEN — Add `GET /api/v1/restaurants/{restaurantId}/owner` to `/Users/juliandiaz/Projects/PRAGMA/Reto1/food-court-service/src/main/java/com/pragma/foodcourt/infrastructure/input/rest/RestaurantRestController.java`.
- [x] 2.5 [fc] RED — `EmployeeUseCaseTest.registerEmployee`: link success, 409 on duplicate, defense-in-depth ownership re-check (EAC-S01, EAC-S08).
- [x] 2.6 [fc] GREEN — Refactor `/Users/juliandiaz/Projects/PRAGMA/Reto1/food-court-service/src/main/java/com/pragma/foodcourt/domain/usecase/EmployeeUseCase.java`: POST body `{"employeeId"}` link-only; remove `IUserCreationPort` call.
- [x] 2.7 [fc] RED — `RestaurantValidatorTest`: ownerId-present null/blank → `RequiredFieldException`.
- [x] 2.8 [fc] GREEN — Add ownerId-present check to `/Users/juliandiaz/Projects/PRAGMA/Reto1/food-court-service/src/main/java/com/pragma/foodcourt/domain/validation/RestaurantValidator.java`.
- [x] 2.9 [fc] Update food-court OpenAPI docs for new `GET /{restaurantId}/owner` and changed `POST /{restaurantId}/employees` (REQ-ROV-06).
- [x] 2.10 [fc] Update `/Users/juliandiaz/Projects/PRAGMA/Reto1/food-court-service/docs/DOMAIN-RUNBOOK.md` with new endpoint + exceptions.

## Slice 3: users-service Employee Creation Relocation [PR 3]

- [x] 3.1 [users] Create `src/main/java/com/pragma/users/domain/spi/IRestaurantOwnershipPort.java` (`getOwnerId`) and `IEmployeeRestaurantLinkPort.java` (`registerEmployee`).
- [x] 3.2 [users] Create `src/main/java/com/pragma/users/domain/exception/RestaurantNotFoundException.java` (404), `NotRestaurantOwnerException.java` (403), `EmployeeLinkRegistrationException.java` (502).
- [x] 3.3 [users] Add `restaurantId UUID` to `src/main/java/com/pragma/users/application/dto/request/CreateEmployeeRequestDto.java`.
- [x] 3.4 [users] RED — `UserUseCaseTest.createEmployee`: success (EAC-S01), 404 missing restaurant (EAC-S06), 403 not owner (EAC-S07), link-fail → compensating delete + 502 (design §flow step 5).
- [x] 3.5 [users] GREEN — Implement `UserUseCase.createEmployee(user, restaurantId, ownerId)` orchestration + compensating delete on link registration failure.
- [x] 3.6 [users] Modify `src/main/java/com/pragma/users/infrastructure/input/rest/UserRestController.java` + `application/handler/impl/UserHandler.java`: use `@AuthenticationPrincipal AuthenticatedUser`, pass `restaurantId` to use case.
- [x] 3.7 [users] RED — `FoodCourtRestClientAdapterTest`: GET 404→`RestaurantNotFoundException`, POST 409→`EmployeeAlreadyExistsException`, `Authorization` header forwarded (EAC-S06, EAC-S07, EAC-S08).
- [x] 3.8 [users] GREEN — Create `src/main/java/com/pragma/users/infrastructure/out/client/FoodCourtRestClientAdapter.java` implementing both SPI ports (MockRestServiceServer).
- [x] 3.9 [users] Create `src/main/java/com/pragma/users/infrastructure/out/client/AuthorizationForwardingInterceptor.java` to propagate owner JWT to food-court.
- [x] 3.10 [users] Modify `src/main/java/com/pragma/users/infrastructure/configuration/BeanConfiguration.java`: wire `foodCourtRestClient` (base-url from props), interceptor, `FoodCourtRestClientAdapter`, `IRestaurantOwnershipPort`, `IEmployeeRestaurantLinkPort`.
- [x] 3.11 [users] Update users-service OpenAPI docs for `POST /api/v1/users/employee` (REQ-EAC-11, all error codes 400/401/403/404/409).
- [x] 3.12 [users] Update `docs/DOMAIN-RUNBOOK.md` with new exceptions table (EAC exception table rows).
- [x] 3.13 [users] Add/verify `src/test/resources/application-test.yml` H2 test profile so `contextLoads()` passes in CI.

## Slice 4: food-court Reverse Adapter Removals + Done Criteria [PR 4]

- [ ] 4.1 [fc] RED — Confirm no outbound HTTP call to users-service on `registerEmployee` or restaurant operations (JI-S05).
- [ ] 4.2 [fc] Delete `/Users/juliandiaz/Projects/PRAGMA/Reto1/food-court-service/src/main/java/com/pragma/foodcourt/domain/spi/IUserCreationPort.java`, `IUserValidationPort.java`.
- [ ] 4.3 [fc] Delete `/Users/juliandiaz/Projects/PRAGMA/Reto1/food-court-service/src/main/java/com/pragma/foodcourt/infrastructure/out/client/UserCreationRestClientAdapter.java`, `UserValidationRestClientAdapter.java`, `CreateEmployeeClientRequest.java`, `UserClientResponse.java`.
- [ ] 4.4 [fc] Delete food-court `AuthorizationForwardingInterceptor.java`; remove `usersRestClient` bean from `/Users/juliandiaz/Projects/PRAGMA/Reto1/food-court-service/src/main/java/com/pragma/foodcourt/infrastructure/configuration/BeanConfiguration.java`.
- [ ] 4.5 [both] SonarLint clean — zero open findings in both repos before raising PRs.
- [ ] 4.6 [both] HU6 completion checklist: name, description, role, criteria verbatim + checklist mapping each criterion + rules (exceptions, validator, runbook, tests, OpenAPI, hexagonal, SonarLint).
