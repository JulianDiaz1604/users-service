package com.pragma.users.domain.usecase;

import com.pragma.users.domain.exception.InvalidCredentialsException;
import com.pragma.users.domain.exception.RequiredFieldException;
import com.pragma.users.domain.model.User;
import com.pragma.users.domain.model.UserRole;
import com.pragma.users.domain.spi.IPasswordEncoderPort;
import com.pragma.users.domain.spi.ITokenProviderPort;
import com.pragma.users.domain.spi.IUserPersistencePort;
import com.pragma.users.domain.validation.AuthValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseTest {

    @Mock
    private IUserPersistencePort userPersistencePort;

    @Mock
    private IPasswordEncoderPort passwordEncoderPort;

    @Mock
    private ITokenProviderPort tokenProviderPort;

    private AuthUseCase authUseCase;
    private User owner;

    @BeforeEach
    void setUp() {
        authUseCase = new AuthUseCase(userPersistencePort, passwordEncoderPort,
                tokenProviderPort, new AuthValidator());
        owner = new User(UUID.randomUUID(), "Ana", "Gomez", "123456789", "+573005698325",
                LocalDate.of(1990, 1, 1), "ana.gomez@example.com", "encodedPassword",
                UserRole.OWNER.getId());
    }

    @Test
    @DisplayName("Returns a signed token when email exists and password matches")
    void shouldReturnTokenWhenCredentialsAreValid() {
        when(userPersistencePort.findByEmail("ana.gomez@example.com")).thenReturn(Optional.of(owner));
        when(passwordEncoderPort.matches("plainPassword", "encodedPassword")).thenReturn(true);
        when(tokenProviderPort.generateToken(owner.getId(), owner.getEmail(), UserRole.OWNER))
                .thenReturn("signed.jwt.token");

        String token = authUseCase.login("ana.gomez@example.com", "plainPassword");

        assertEquals("signed.jwt.token", token);
        verify(tokenProviderPort).generateToken(owner.getId(), owner.getEmail(), UserRole.OWNER);
    }

    @Test
    @DisplayName("Rejects login when the email is not registered")
    void shouldThrowWhenEmailDoesNotExist() {
        when(userPersistencePort.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authUseCase.login("missing@example.com", "plainPassword"));
        verify(tokenProviderPort, never()).generateToken(any(), anyString(), any());
    }

    @Test
    @DisplayName("Rejects login when the password does not match")
    void shouldThrowWhenPasswordDoesNotMatch() {
        when(userPersistencePort.findByEmail("ana.gomez@example.com")).thenReturn(Optional.of(owner));
        when(passwordEncoderPort.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authUseCase.login("ana.gomez@example.com", "wrongPassword"));
        verify(tokenProviderPort, never()).generateToken(any(), anyString(), any());
    }

    @Test
    @DisplayName("Rejects login when a required field is blank")
    void shouldThrowWhenCredentialFieldIsBlank() {
        assertThrows(RequiredFieldException.class, () -> authUseCase.login("", "plainPassword"));
        verify(userPersistencePort, never()).findByEmail(anyString());
    }
}
