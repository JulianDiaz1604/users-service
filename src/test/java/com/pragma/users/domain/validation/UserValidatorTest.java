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

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserValidatorTest {

    private static final LocalDate REFERENCE_DATE = LocalDate.of(2025, 1, 1);

    private final Clock fixedClock =
            Clock.fixed(REFERENCE_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    private final UserValidator userValidator = new UserValidator(fixedClock);
    private User validOwner;
    private User validEmployee;
    private User validClient;

    @BeforeEach
    void setUp() {
        validOwner = new User(null, "Ana", "Gomez", "123456789", "+573005698325",
                LocalDate.of(1990, 1, 1), "ana.gomez@example.com", "plainPassword", null);
        validEmployee = new User(null, "Luis", "Perez", "987654321", "+573001112233",
                null, "luis.perez@example.com", "plainPassword", null);
        validClient = new User(null, "Marta", "Ruiz", "1122334455", "+573004445566",
                null, "marta.ruiz@example.com", "plainPassword", null);
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
        validOwner.setBirthDate(REFERENCE_DATE.minusYears(17));
        assertThrows(UnderAgeException.class, () -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Accepts an owner who turns adult exactly on the reference date")
    void shouldAcceptOwnerExactlyAtAdultBoundary() {
        validOwner.setBirthDate(REFERENCE_DATE.minusYears(18));
        assertDoesNotThrow(() -> userValidator.validateNewOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects an owner one day short of adulthood")
    void shouldThrowWhenOwnerIsOneDayShortOfAdulthood() {
        validOwner.setBirthDate(REFERENCE_DATE.minusYears(18).plusDays(1));
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

    @Test
    @DisplayName("Accepts a valid client")
    void shouldAcceptValidClient() {
        assertDoesNotThrow(() -> userValidator.validateNewClient(validClient));
    }

    @Test
    @DisplayName("Accepts a client without a birth date (not required for clients)")
    void shouldAcceptClientWithoutBirthDate() {
        validClient.setBirthDate(null);
        assertDoesNotThrow(() -> userValidator.validateNewClient(validClient));
    }

    @Test
    @DisplayName("Does not enforce the adult-age rule on clients")
    void shouldNotEnforceAgeRuleOnClient() {
        validClient.setBirthDate(LocalDate.now().minusYears(17));
        assertDoesNotThrow(() -> userValidator.validateNewClient(validClient));
    }

    @Test
    @DisplayName("Rejects a client with a blank required field")
    void shouldThrowWhenClientRequiredFieldIsBlank() {
        validClient.setName("  ");
        assertThrows(RequiredFieldException.class, () -> userValidator.validateNewClient(validClient));
    }

    @Test
    @DisplayName("Rejects a client with an invalid email")
    void shouldThrowWhenClientEmailIsInvalid() {
        validClient.setEmail("not-an-email");
        assertThrows(InvalidEmailFormatException.class, () -> userValidator.validateNewClient(validClient));
    }

    @Test
    @DisplayName("Rejects a client with a phone longer than 13 characters")
    void shouldThrowWhenClientPhoneTooLong() {
        validClient.setPhone("+5730044455667");
        assertThrows(InvalidPhoneFormatException.class, () -> userValidator.validateNewClient(validClient));
    }

    @Test
    @DisplayName("Rejects a client with a non-numeric document number")
    void shouldThrowWhenClientDocumentIsNonNumeric() {
        validClient.setDocumentNumber("ABC123");
        assertThrows(InvalidDocumentNumberException.class, () -> userValidator.validateNewClient(validClient));
    }
}
