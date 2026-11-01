package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.exception.DomainException;
import com.pragma.powerup.domain.exception.UserAlreadyExistsException;
import com.pragma.powerup.domain.model.User;
import com.pragma.powerup.domain.model.UserRole;
import com.pragma.powerup.domain.spi.IPasswordEncoderPort;
import com.pragma.powerup.domain.spi.IUserPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUseCaseTest {

    @Mock
    private IUserPersistencePort userPersistencePort;

    @Mock
    private IPasswordEncoderPort passwordEncoderPort;

    @InjectMocks
    private UserUseCase userUseCase;

    private User validOwner;

    @BeforeEach
    void setUp() {
        validOwner = new User(
                null,
                "Ana",
                "Gomez",
                "123456789",
                "+573005698325",
                LocalDate.of(1990, 1, 1),
                "ana.gomez@example.com",
                "plainPassword",
                null);
    }

    @Test
    @DisplayName("Creates an owner with encoded password and OWNER role")
    void createOwner_persistsOwnerWithEncodedPasswordAndOwnerRole() {
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(false);
        when(userPersistencePort.existsByDocumentNumber(anyString())).thenReturn(false);
        when(passwordEncoderPort.encode("plainPassword")).thenReturn("encodedPassword");
        when(userPersistencePort.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userUseCase.createOwner(validOwner);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userPersistencePort).saveUser(captor.capture());
        User saved = captor.getValue();
        assertEquals(UserRole.OWNER.getId(), saved.getRoleId());
        assertEquals("encodedPassword", saved.getPassword());
        assertEquals("ana.gomez@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Rejects a blank required field")
    void createOwner_withBlankName_throwsDomainException() {
        validOwner.setName("  ");
        assertThrows(DomainException.class, () -> userUseCase.createOwner(validOwner));
        verify(userPersistencePort, never()).saveUser(any());
    }

    @Test
    @DisplayName("Rejects an invalid email format")
    void createOwner_withInvalidEmail_throwsDomainException() {
        validOwner.setEmail("not-an-email");
        assertThrows(DomainException.class, () -> userUseCase.createOwner(validOwner));
        verify(userPersistencePort, never()).saveUser(any());
    }

    @Test
    @DisplayName("Rejects a phone longer than 13 characters")
    void createOwner_withTooLongPhone_throwsDomainException() {
        validOwner.setPhone("+5730056983251");
        assertThrows(DomainException.class, () -> userUseCase.createOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects a non-numeric document number")
    void createOwner_withNonNumericDocument_throwsDomainException() {
        validOwner.setDocumentNumber("ABC123");
        assertThrows(DomainException.class, () -> userUseCase.createOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects an underage owner")
    void createOwner_whenUnderage_throwsDomainException() {
        validOwner.setBirthDate(LocalDate.now().minusYears(17));
        assertThrows(DomainException.class, () -> userUseCase.createOwner(validOwner));
    }

    @Test
    @DisplayName("Rejects a duplicated email")
    void createOwner_whenEmailExists_throwsUserAlreadyExistsException() {
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(true);
        assertThrows(UserAlreadyExistsException.class, () -> userUseCase.createOwner(validOwner));
        verify(userPersistencePort, never()).saveUser(any());
    }

    @Test
    @DisplayName("Rejects a duplicated document number")
    void createOwner_whenDocumentExists_throwsUserAlreadyExistsException() {
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(false);
        when(userPersistencePort.existsByDocumentNumber(anyString())).thenReturn(true);
        assertThrows(UserAlreadyExistsException.class, () -> userUseCase.createOwner(validOwner));
        verify(userPersistencePort, never()).saveUser(any());
    }
}
