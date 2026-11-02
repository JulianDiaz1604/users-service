package com.pragma.users.domain.validation;

import com.pragma.users.domain.exception.InvalidDocumentNumberException;
import com.pragma.users.domain.exception.InvalidEmailFormatException;
import com.pragma.users.domain.exception.InvalidPhoneFormatException;
import com.pragma.users.domain.exception.RequiredFieldException;
import com.pragma.users.domain.exception.UnderAgeException;
import com.pragma.users.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserValidatorTest {

    private UserValidator userValidator;
    private User validOwner;

    @BeforeEach
    void setUp() {
        userValidator = new UserValidator();
        validOwner = new User(null, "Ana", "Gomez", "123456789", "+573005698325",
                LocalDate.of(1990, 1, 1), "ana.gomez@example.com", "plainPassword", null);
    }

    @Test
    @DisplayName("Accepts a valid owner")
    void validateNewOwner_withValidData_doesNotThrow() {
        assertDoesNotThrow(() -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects a blank required field")
    void validateNewOwner_withBlankName_throwsRequiredFieldException() {
        validOwner.setName("  ");
        assertThrows(RequiredFieldException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects a null birth date")
    void validateNewOwner_withNullBirthDate_throwsRequiredFieldException() {
        validOwner.setBirthDate(null);
        assertThrows(RequiredFieldException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects an invalid email")
    void validateNewOwner_withInvalidEmail_throwsInvalidEmailFormatException() {
        validOwner.setEmail("not-an-email");
        assertThrows(InvalidEmailFormatException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects a phone longer than 13 characters")
    void validateNewOwner_withTooLongPhone_throwsInvalidPhoneFormatException() {
        validOwner.setPhone("+5730056983251");
        assertThrows(InvalidPhoneFormatException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects a phone with letters")
    void validateNewOwner_withNonNumericPhone_throwsInvalidPhoneFormatException() {
        validOwner.setPhone("abc123");
        assertThrows(InvalidPhoneFormatException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects a non-numeric document number")
    void validateNewOwner_withNonNumericDocument_throwsInvalidDocumentNumberException() {
        validOwner.setDocumentNumber("ABC123");
        assertThrows(InvalidDocumentNumberException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects an underage owner")
    void validateNewOwner_whenUnderage_throwsUnderAgeException() {
        validOwner.setBirthDate(LocalDate.now().minusYears(17));
        assertThrows(UnderAgeException.class, () -> userValidator.validateNewOwner(validOwner));
    }
}
