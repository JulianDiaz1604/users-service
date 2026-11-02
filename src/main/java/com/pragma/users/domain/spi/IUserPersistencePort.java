package com.pragma.users.domain.spi;

import com.pragma.users.domain.model.User;

import java.util.UUID;

public interface IUserPersistencePort {
    User saveUser(User user);

    boolean existsByEmail(String email);

    boolean existsByDocumentNumber(String documentNumber);

    User getUserById(UUID id);
}
