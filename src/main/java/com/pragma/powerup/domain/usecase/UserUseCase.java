package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.api.IUserServicePort;
import com.pragma.powerup.domain.exception.DocumentNumberAlreadyExistsException;
import com.pragma.powerup.domain.exception.EmailAlreadyExistsException;
import com.pragma.powerup.domain.model.User;
import com.pragma.powerup.domain.model.UserRole;
import com.pragma.powerup.domain.spi.IPasswordEncoderPort;
import com.pragma.powerup.domain.spi.IUserPersistencePort;
import com.pragma.powerup.domain.validation.UserValidator;

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
