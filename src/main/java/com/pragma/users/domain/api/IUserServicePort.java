package com.pragma.users.domain.api;

import com.pragma.users.domain.model.User;

import java.util.UUID;

public interface IUserServicePort {
    User createOwner(User user);

    User getUserById(UUID id);
}
