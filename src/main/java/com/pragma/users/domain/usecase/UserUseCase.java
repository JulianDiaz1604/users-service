package com.pragma.users.domain.usecase;

import com.pragma.users.domain.api.IUserServicePort;
import com.pragma.users.domain.exception.DocumentNumberAlreadyExistsException;
import com.pragma.users.domain.exception.EmailAlreadyExistsException;
import com.pragma.users.domain.exception.EmployeeAlreadyExistsException;
import com.pragma.users.domain.exception.EmployeeLinkRegistrationException;
import com.pragma.users.domain.exception.NotRestaurantOwnerException;
import com.pragma.users.domain.model.User;
import com.pragma.users.domain.model.UserRole;
import com.pragma.users.domain.spi.IEmployeeRestaurantLinkPort;
import com.pragma.users.domain.spi.IPasswordEncoderPort;
import com.pragma.users.domain.spi.IRestaurantOwnershipPort;
import com.pragma.users.domain.spi.IUserPersistencePort;
import com.pragma.users.domain.validation.UserValidator;

import java.util.UUID;

public class UserUseCase implements IUserServicePort {

    private final IUserPersistencePort userPersistencePort;
    private final IPasswordEncoderPort passwordEncoderPort;
    private final UserValidator userValidator;
    private final IRestaurantOwnershipPort restaurantOwnershipPort;
    private final IEmployeeRestaurantLinkPort employeeRestaurantLinkPort;

    public UserUseCase(IUserPersistencePort userPersistencePort,
                       IPasswordEncoderPort passwordEncoderPort,
                       UserValidator userValidator,
                       IRestaurantOwnershipPort restaurantOwnershipPort,
                       IEmployeeRestaurantLinkPort employeeRestaurantLinkPort) {
        this.userPersistencePort = userPersistencePort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.userValidator = userValidator;
        this.restaurantOwnershipPort = restaurantOwnershipPort;
        this.employeeRestaurantLinkPort = employeeRestaurantLinkPort;
    }

    @Override
    public User createOwner(User user) {
        userValidator.validateNewOwner(user);
        return persistWithRole(user, UserRole.OWNER);
    }

    /**
     * Orchestration (design §Data Flow):
     * 1. Validate employee fields (no birthDate/age rule).
     * 2. GET ownership — fail fast, no side effects.
     * 3. Assert requesterOwnerId == ownerId.
     * 4. Persist account (bcrypt + uniqueness checks).
     * 5. Register employee-restaurant link.
     *    - 409 from link → rethrow EmployeeAlreadyExistsException (no compensation).
     *    - Other failures → compensating delete + EmployeeLinkRegistrationException (502).
     */
    @Override
    public User createEmployee(User user, UUID restaurantId, UUID requesterOwnerId) {
        userValidator.validateNewEmployee(user);

        // Step 2: ownership check (throws RestaurantNotFoundException on 404)
        UUID ownerId = restaurantOwnershipPort.getOwnerId(restaurantId);

        // Step 3: authorization check
        if (!ownerId.equals(requesterOwnerId)) {
            throw new NotRestaurantOwnerException();
        }

        // Step 4: persist account
        User savedEmployee = persistWithRole(user, UserRole.EMPLOYEE);

        // Step 5: register link
        try {
            employeeRestaurantLinkPort.registerEmployee(restaurantId, savedEmployee.getId());
        } catch (EmployeeAlreadyExistsException e) {
            // 409 from food-court — no compensation; rethrow as-is
            throw e;
        } catch (RuntimeException e) {
            // Any other failure — compensate by deleting the just-created account
            userPersistencePort.deleteUser(savedEmployee.getId());
            throw new EmployeeLinkRegistrationException();
        }

        return savedEmployee;
    }

    @Override
    public User createClient(User user) {
        userValidator.validateNewClient(user);
        return persistWithRole(user, UserRole.CLIENT);
    }

    private User persistWithRole(User user, UserRole role) {
        ensureEmailIsAvailable(user.getEmail());
        ensureDocumentNumberIsAvailable(user.getDocumentNumber());

        user.setRoleId(role.getId());
        user.setPassword(passwordEncoderPort.encode(user.getPassword()));
        return userPersistencePort.saveUser(user);
    }

    @Override
    public User getUserById(UUID id) {
        return userPersistencePort.getUserById(id);
    }

    private void ensureEmailIsAvailable(String email) {
        if (userPersistencePort.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
    }

    private void ensureDocumentNumberIsAvailable(String documentNumber) {
        if (userPersistencePort.existsByDocumentNumber(documentNumber)) {
            throw new DocumentNumberAlreadyExistsException();
        }
    }
}
