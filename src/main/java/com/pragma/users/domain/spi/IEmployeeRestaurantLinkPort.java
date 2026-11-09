package com.pragma.users.domain.spi;

import java.util.UUID;

/**
 * Driven port: registers the employee-restaurant association in the restaurant service.
 * The link record is persisted in the restaurant service's data store, not in users-service.
 *
 * <p>Throws {@link com.pragma.users.domain.exception.EmployeeAlreadyExistsException} on a
 * duplicate link (HTTP 409 from the restaurant service). Any other failure propagates as a
 * RuntimeException, allowing the caller to perform compensating actions.</p>
 */
public interface IEmployeeRestaurantLinkPort {

    /**
     * Registers the employee-restaurant link.
     *
     * @param restaurantId the UUID of the restaurant
     * @param employeeId   the UUID of the newly created employee account
     * @throws com.pragma.users.domain.exception.EmployeeAlreadyExistsException if the link
     *         already exists (409 from the restaurant service)
     */
    void registerEmployee(UUID restaurantId, UUID employeeId);
}
