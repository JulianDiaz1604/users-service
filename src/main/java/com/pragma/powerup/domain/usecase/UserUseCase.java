package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.api.IUserServicePort;
import com.pragma.powerup.domain.exception.DomainException;
import com.pragma.powerup.domain.exception.UserAlreadyExistsException;
import com.pragma.powerup.domain.model.User;
import com.pragma.powerup.domain.model.UserRole;
import com.pragma.powerup.domain.spi.IPasswordEncoderPort;
import com.pragma.powerup.domain.spi.IUserPersistencePort;
import com.pragma.powerup.domain.util.DomainConstants;

import java.time.LocalDate;
import java.time.Period;

public class UserUseCase implements IUserServicePort {

    private final IUserPersistencePort userPersistencePort;
    private final IPasswordEncoderPort passwordEncoderPort;

    public UserUseCase(IUserPersistencePort userPersistencePort, IPasswordEncoderPort passwordEncoderPort) {
        this.userPersistencePort = userPersistencePort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public User createOwner(User user) {
        validateRequiredFields(user);
        validateEmail(user.getEmail());
        validatePhone(user.getPhone());
        validateDocument(user.getDocumentNumber());
        validateAge(user.getBirthDate());
        validateUniqueness(user);

        user.setRoleId(UserRole.OWNER.getId());
        user.setPassword(passwordEncoderPort.encode(user.getPassword()));
        return userPersistencePort.saveUser(user);
    }

    private void validateRequiredFields(User user) {
        if (isBlank(user.getName())
                || isBlank(user.getLastName())
                || isBlank(user.getDocumentNumber())
                || isBlank(user.getPhone())
                || isBlank(user.getEmail())
                || isBlank(user.getPassword())
                || user.getBirthDate() == null) {
            throw new DomainException(DomainConstants.FIELD_REQUIRED_MESSAGE);
        }
    }

    private void validateEmail(String email) {
        if (!email.matches(DomainConstants.EMAIL_REGEX)) {
            throw new DomainException(DomainConstants.INVALID_EMAIL_MESSAGE);
        }
    }

    private void validatePhone(String phone) {
        if (phone.length() > DomainConstants.PHONE_MAX_LENGTH || !phone.matches(DomainConstants.PHONE_REGEX)) {
            throw new DomainException(DomainConstants.INVALID_PHONE_MESSAGE);
        }
    }

    private void validateDocument(String documentNumber) {
        if (!documentNumber.matches(DomainConstants.DOCUMENT_REGEX)) {
            throw new DomainException(DomainConstants.INVALID_DOCUMENT_MESSAGE);
        }
    }

    private void validateAge(LocalDate birthDate) {
        if (Period.between(birthDate, LocalDate.now()).getYears() < DomainConstants.MINIMUM_AGE) {
            throw new DomainException(DomainConstants.UNDERAGE_MESSAGE);
        }
    }

    private void validateUniqueness(User user) {
        if (userPersistencePort.existsByEmail(user.getEmail())) {
            throw new UserAlreadyExistsException(DomainConstants.EMAIL_ALREADY_EXISTS_MESSAGE);
        }
        if (userPersistencePort.existsByDocumentNumber(user.getDocumentNumber())) {
            throw new UserAlreadyExistsException(DomainConstants.DOCUMENT_ALREADY_EXISTS_MESSAGE);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
