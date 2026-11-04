package com.pragma.users.domain.usecase;

import com.pragma.users.domain.exception.DocumentNumberAlreadyExistsException;
import com.pragma.users.domain.exception.EmailAlreadyExistsException;
import com.pragma.users.domain.model.User;
import com.pragma.users.domain.model.UserRole;
import com.pragma.users.domain.spi.IPasswordEncoderPort;
import com.pragma.users.domain.spi.IUserPersistencePort;
import com.pragma.users.domain.validation.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

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

    private UserUseCase userUseCase;
    private User validOwner;
    private User validEmployee;
    private User validClient;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        userUseCase = new UserUseCase(userPersistencePort, passwordEncoderPort, new UserValidator(fixedClock));
        validOwner = new User(null, "Ana", "Gomez", "123456789", "+573005698325",
                LocalDate.of(1990, 1, 1), "ana.gomez@example.com", "plainPassword", null);
        validEmployee = new User(null, "Luis", "Perez", "987654321", "+573001112233",
                null, "luis.perez@example.com", "plainPassword", null);
        validClient = new User(null, "Marta", "Ruiz", "1122334455", "+573004445566",
                null, "marta.ruiz@example.com", "plainPassword", null);
    }

    @Test
    @DisplayName("Creates an owner with encoded password and OWNER role")
    void shouldCreateOwnerWithEncodedPasswordAndOwnerRole() {
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
    @DisplayName("Rejects a duplicated email")
    void shouldThrowWhenEmailAlreadyExists() {
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(true);
        assertThrows(EmailAlreadyExistsException.class, () -> userUseCase.createOwner(validOwner));
        verify(userPersistencePort, never()).saveUser(any());
    }

    @Test
    @DisplayName("Rejects a duplicated document number")
    void shouldThrowWhenDocumentAlreadyExists() {
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(false);
        when(userPersistencePort.existsByDocumentNumber(anyString())).thenReturn(true);
        assertThrows(DocumentNumberAlreadyExistsException.class, () -> userUseCase.createOwner(validOwner));
        verify(userPersistencePort, never()).saveUser(any());
    }

    @Test
    @DisplayName("Creates an employee with encoded password and EMPLOYEE role")
    void shouldCreateEmployeeWithEncodedPasswordAndEmployeeRole() {
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(false);
        when(userPersistencePort.existsByDocumentNumber(anyString())).thenReturn(false);
        when(passwordEncoderPort.encode("plainPassword")).thenReturn("encodedPassword");
        when(userPersistencePort.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userUseCase.createEmployee(validEmployee);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userPersistencePort).saveUser(captor.capture());
        User saved = captor.getValue();
        assertEquals(UserRole.EMPLOYEE.getId(), saved.getRoleId());
        assertEquals("encodedPassword", saved.getPassword());
        assertEquals("luis.perez@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Rejects a duplicated email when creating an employee")
    void shouldThrowWhenEmployeeEmailAlreadyExists() {
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(true);
        assertThrows(EmailAlreadyExistsException.class, () -> userUseCase.createEmployee(validEmployee));
        verify(userPersistencePort, never()).saveUser(any());
    }

    @Test
    @DisplayName("Rejects a duplicated document number when creating an employee")
    void shouldThrowWhenEmployeeDocumentAlreadyExists() {
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(false);
        when(userPersistencePort.existsByDocumentNumber(anyString())).thenReturn(true);
        assertThrows(DocumentNumberAlreadyExistsException.class, () -> userUseCase.createEmployee(validEmployee));
        verify(userPersistencePort, never()).saveUser(any());
    }

    @Test
    @DisplayName("Creates a client with encoded password and CLIENT role")
    void shouldCreateClientWithEncodedPasswordAndClientRole() {
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(false);
        when(userPersistencePort.existsByDocumentNumber(anyString())).thenReturn(false);
        when(passwordEncoderPort.encode("plainPassword")).thenReturn("encodedPassword");
        when(userPersistencePort.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userUseCase.createClient(validClient);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userPersistencePort).saveUser(captor.capture());
        User saved = captor.getValue();
        assertEquals(UserRole.CLIENT.getId(), saved.getRoleId());
        assertEquals("encodedPassword", saved.getPassword());
        assertEquals("marta.ruiz@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Rejects a duplicated email when creating a client")
    void shouldThrowWhenClientEmailAlreadyExists() {
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(true);
        assertThrows(EmailAlreadyExistsException.class, () -> userUseCase.createClient(validClient));
        verify(userPersistencePort, never()).saveUser(any());
    }

    @Test
    @DisplayName("Rejects a duplicated document number when creating a client")
    void shouldThrowWhenClientDocumentAlreadyExists() {
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(false);
        when(userPersistencePort.existsByDocumentNumber(anyString())).thenReturn(true);
        assertThrows(DocumentNumberAlreadyExistsException.class, () -> userUseCase.createClient(validClient));
        verify(userPersistencePort, never()).saveUser(any());
    }

    @Test
    @DisplayName("Returns the user retrieved by id from the persistence port")
    void shouldReturnUserRetrievedByIdFromPersistencePort() {
        UUID userId = UUID.randomUUID();
        User persisted = new User(userId, "Ana", "Gomez", "123456789", "+573005698325",
                LocalDate.of(1990, 1, 1), "ana.gomez@example.com", "encodedPassword", UserRole.OWNER.getId());
        when(userPersistencePort.getUserById(userId)).thenReturn(persisted);

        User result = userUseCase.getUserById(userId);

        assertEquals(userId, result.getId());
        assertEquals(UserRole.OWNER.getId(), result.getRoleId());
        verify(userPersistencePort).getUserById(userId);
    }
}
