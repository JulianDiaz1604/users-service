package com.pragma.powerup.domain.spi;

import com.pragma.powerup.domain.model.User;

public interface IUserPersistencePort {
    User saveUser(User user);

    boolean existsByEmail(String email);

    boolean existsByDocumentNumber(String documentNumber);
}
