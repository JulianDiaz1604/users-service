package com.pragma.users.domain.spi;

import com.pragma.users.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface IUserPersistencePort {
    User saveUser(User user);

    boolean existsByEmail(String email);

    boolean existsByDocumentNumber(String documentNumber);

    User getUserById(UUID id);

    Optional<User> findByEmail(String email);

    /**
     * Deletes the user with the given id. Used for compensating transactions when
     * downstream link registration fails after the account has been persisted.
     *
     * @param id the UUID of the user to delete
     */
    void deleteUser(UUID id);
}
