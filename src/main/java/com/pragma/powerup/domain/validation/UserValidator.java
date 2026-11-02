package com.pragma.powerup.domain.validation;

import com.pragma.powerup.domain.exception.InvalidDocumentNumberException;
import com.pragma.powerup.domain.exception.InvalidEmailFormatException;
import com.pragma.powerup.domain.exception.InvalidPhoneFormatException;
import com.pragma.powerup.domain.exception.RequiredFieldException;
import com.pragma.powerup.domain.exception.UnderAgeException;
import com.pragma.powerup.domain.model.User;
import com.pragma.powerup.domain.util.DomainConstants;

import java.time.LocalDate;
import java.time.Period;

public class UserValidator {

    public void validateNewOwner(User user) {
        validateRequiredFields(user);
        validateEmailFormat(user.getEmail());
        validatePhoneFormat(user.getPhone());
        validateDocumentNumber(user.getDocumentNumber());
        validateAdultAge(user.getBirthDate());
    }

    private void validateRequiredFields(User user) {
        if (isBlank(user.getName())
                || isBlank(user.getLastName())
                || isBlank(user.getDocumentNumber())
                || isBlank(user.getPhone())
                || isBlank(user.getEmail())
                || isBlank(user.getPassword())
                || user.getBirthDate() == null) {
            throw new RequiredFieldException();
        }
    }

    private void validateEmailFormat(String email) {
        if (!email.matches(DomainConstants.EMAIL_REGEX)) {
            throw new InvalidEmailFormatException();
        }
    }

    private void validatePhoneFormat(String phone) {
        if (phone.length() > DomainConstants.PHONE_MAX_LENGTH || !phone.matches(DomainConstants.PHONE_REGEX)) {
            throw new InvalidPhoneFormatException();
        }
    }

    private void validateDocumentNumber(String documentNumber) {
        if (!documentNumber.matches(DomainConstants.DOCUMENT_REGEX)) {
            throw new InvalidDocumentNumberException();
        }
    }

    private void validateAdultAge(LocalDate birthDate) {
        if (Period.between(birthDate, LocalDate.now()).getYears() < DomainConstants.MINIMUM_AGE) {
            throw new UnderAgeException();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
