package com.pragma.users.domain.usecase;

import com.pragma.users.domain.api.IUserServicePort;
import com.pragma.users.domain.exception.DocumentNumberAlreadyExistsException;
import com.pragma.users.domain.exception.EmailAlreadyExistsException;
import com.pragma.users.domain.model.User;
import com.pragma.users.domain.model.UserRole;
import com.pragma.users.domain.spi.IPasswordEncoderPort;
import com.pragma.users.domain.spi.IUserPersistencePort;
import com.pragma.users.domain.validation.UserValidator;

import java.util.UUID;

public class UserUseCase implements IUserServicePort {

    private final IUserPersistencePort userPersistencePort;
    private final IPasswordEncoderPort passwordEncoderPort;
    private final UserValidator userValidator;

    public UserUseCase(IUserPersistencePort userPersistencePort,
                       IPasswordEncoderPort passwordEncoderPort,
                       UserValidator userValidator) {
        this.userPersistencePort = userPersistencePort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.userValidator = userValidator;
    }

    @Override
    public User createOwner(User user) {
        userValidator.validateNewOwner(user);
        ensureEmailIsAvailable(user.getEmail());
        ensureDocumentNumberIsAvailable(user.getDocumentNumber());

        user.setRoleId(UserRole.OWNER.getId());
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
