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

    private final UserValidator userValidator = new UserValidator();
    private User validOwner;
    private User validEmployee;

    @BeforeEach
    void setUp() {
        validOwner = new User(null, "Ana", "Gomez", "123456789", "+573005698325",
                LocalDate.of(1990, 1, 1), "ana.gomez@example.com", "plainPassword", null);
        validEmployee = new User(null, "Luis", "Perez", "987654321", "+573001112233",
                null, "luis.perez@example.com", "plainPassword", null);
    }

    @Test
    @DisplayName("Accepts a valid owner")
    void shouldAcceptValidOwner() {
        assertDoesNotThrow(() -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects a blank required field")
    void shouldThrowWhenRequiredFieldIsBlank() {
        validOwner.setName("  ");
        assertThrows(RequiredFieldException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects a null birth date")
    void shouldThrowWhenBirthDateIsNull() {
        validOwner.setBirthDate(null);
        assertThrows(RequiredFieldException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects an invalid email")
    void shouldThrowWhenEmailIsInvalid() {
        validOwner.setEmail("not-an-email");
        assertThrows(InvalidEmailFormatException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects a phone longer than 13 characters")
    void shouldThrowWhenPhoneTooLong() {
        validOwner.setPhone("+5730056983251");
        assertThrows(InvalidPhoneFormatException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects a phone with letters")
    void shouldThrowWhenPhoneHasLetters() {
        validOwner.setPhone("abc123");
        assertThrows(InvalidPhoneFormatException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects a non-numeric document number")
    void shouldThrowWhenDocumentIsNonNumeric() {
        validOwner.setDocumentNumber("ABC123");
        assertThrows(InvalidDocumentNumberException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects an underage owner")
    void shouldThrowWhenOwnerIsUnderage() {
        validOwner.setBirthDate(LocalDate.now().minusYears(17));
        assertThrows(UnderAgeException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Accepts a valid employee")
    void shouldAcceptValidEmployee() {
        assertDoesNotThrow(() -> userValidator.validateNewEmployee(validEmployee));
    }

    @Test
    @DisplayName("Accepts an employee without a birth date (not required for employees)")
    void shouldAcceptEmployeeWithoutBirthDate() {
        validEmployee.setBirthDate(null);
        assertDoesNotThrow(() -> userValidator.validateNewEmployee(validEmployee));
    }

    @Test
    @DisplayName("Does not enforce the adult-age rule on employees")
    void shouldNotEnforceAgeRuleOnEmployee() {
        validEmployee.setBirthDate(LocalDate.now().minusYears(17));
        assertDoesNotThrow(() -> userValidator.validateNewEmployee(validEmployee));
    }

    @Test
    @DisplayName("Rejects an employee with a blank required field")
    void shouldThrowWhenEmployeeRequiredFieldIsBlank() {
        validEmployee.setName("  ");
        assertThrows(RequiredFieldException.class, () -> userValidator.validateNewEmployee(validEmployee));
    }

    @Test
    @DisplayName("Rejects an employee with an invalid email")
    void shouldThrowWhenEmployeeEmailIsInvalid() {
        validEmployee.setEmail("not-an-email");
        assertThrows(InvalidEmailFormatException.class, () -> userValidator.validateNewEmployee(validEmployee));
    }

    @Test
    @DisplayName("Rejects an employee with a phone longer than 13 characters")
    void shouldThrowWhenEmployeePhoneTooLong() {
        validEmployee.setPhone("+5730011122334");
        assertThrows(InvalidPhoneFormatException.class, () -> userValidator.validateNewEmployee(validEmployee));
    }

    @Test
    @DisplayName("Rejects an employee with a non-numeric document number")
    void shouldThrowWhenEmployeeDocumentIsNonNumeric() {
        validEmployee.setDocumentNumber("ABC123");
        assertThrows(InvalidDocumentNumberException.class, () -> userValidator.validateNewEmployee(validEmployee));
    }
}
