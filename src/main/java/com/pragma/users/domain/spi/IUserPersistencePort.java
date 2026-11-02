package com.pragma.users.domain.spi;

import com.pragma.users.domain.model.User;

public interface IUserPersistencePort {
    User saveUser(User user);

    boolean existsByEmail(String email);

    boolean existsByDocumentNumber(String documentNumber);

    User getUserById(Long id);
}
