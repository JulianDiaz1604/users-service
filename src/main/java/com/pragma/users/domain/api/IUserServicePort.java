package com.pragma.users.domain.api;

import com.pragma.users.domain.model.User;

import java.util.UUID;

public interface IUserServicePort {
    User createOwner(User user);

    /**
     * Creates an employee account, verifies restaurant ownership, and registers the
     * employee-restaurant link in the restaurant service.
     *
     * @param user              the employee data (name, lastName, documentNumber, phone, email, password)
     * @param restaurantId      the UUID of the restaurant the employee will work at
     * @param requesterOwnerId  the UUID of the authenticated caller (from the JWT subject)
     * @return the persisted employee (password excluded)
     */
    User createEmployee(User user, UUID restaurantId, UUID requesterOwnerId);

    User createClient(User user);

    User getUserById(UUID id);
}
