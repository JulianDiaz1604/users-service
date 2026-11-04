package com.pragma.users.domain.validation;

import com.pragma.users.domain.exception.RequiredFieldException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthValidatorTest {

    private AuthValidator authValidator;

    @BeforeEach
    void setUp() {
        authValidator = new AuthValidator();
    }

    @Test
    @DisplayName("Passes when both email and password are provided")
    void shouldPassWhenCredentialsArePresent() {
        assertDoesNotThrow(() -> authValidator.validateLoginRequest("user@example.com", "secret"));
    }

    @Test
    @DisplayName("Fails when the email is null")
    void shouldThrowWhenEmailIsNull() {
        assertThrows(RequiredFieldException.class,
                () -> authValidator.validateLoginRequest(null, "secret"));
    }

    @Test
    @DisplayName("Fails when the password is blank")
    void shouldThrowWhenPasswordIsBlank() {
        assertThrows(RequiredFieldException.class,
                () -> authValidator.validateLoginRequest("user@example.com", "   "));
    }
}
