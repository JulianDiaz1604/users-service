package com.pragma.users.domain.api;

import com.pragma.users.domain.model.User;

public interface IUserServicePort {
    User createOwner(User user);
}
