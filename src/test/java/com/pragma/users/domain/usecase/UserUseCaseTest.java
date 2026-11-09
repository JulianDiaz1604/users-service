package com.pragma.users.domain.usecase;

import com.pragma.users.domain.exception.DocumentNumberAlreadyExistsException;
import com.pragma.users.domain.exception.EmailAlreadyExistsException;
import com.pragma.users.domain.exception.EmployeeAlreadyExistsException;
import com.pragma.users.domain.exception.EmployeeLinkRegistrationException;
import com.pragma.users.domain.exception.NotRestaurantOwnerException;
import com.pragma.users.domain.exception.RestaurantNotFoundException;
import com.pragma.users.domain.model.User;
import com.pragma.users.domain.model.UserRole;
import com.pragma.users.domain.spi.IEmployeeRestaurantLinkPort;
import com.pragma.users.domain.spi.IPasswordEncoderPort;
import com.pragma.users.domain.spi.IRestaurantOwnershipPort;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUseCaseTest {

    @Mock
    private IUserPersistencePort userPersistencePort;

    @Mock
    private IPasswordEncoderPort passwordEncoderPort;

    @Mock
    private IRestaurantOwnershipPort restaurantOwnershipPort;

    @Mock
    private IEmployeeRestaurantLinkPort employeeRestaurantLinkPort;

    private UserUseCase userUseCase;
    private User validOwner;
    private User validEmployee;
    private User validClient;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        userUseCase = new UserUseCase(userPersistencePort, passwordEncoderPort,
                new UserValidator(fixedClock), restaurantOwnershipPort, employeeRestaurantLinkPort);
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

    // ---- createEmployee (EAC-S01, EAC-S06, EAC-S07, compensation) ----

    @Test
    @DisplayName("EAC-S01: Creates an employee with encoded password, EMPLOYEE role, and registers the link")
    void shouldCreateEmployeeWithEncodedPasswordAndEmployeeRoleAndRegisterLink() {
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(restaurantOwnershipPort.getOwnerId(restaurantId)).thenReturn(ownerId);
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(false);
        when(userPersistencePort.existsByDocumentNumber(anyString())).thenReturn(false);
        when(passwordEncoderPort.encode("plainPassword")).thenReturn("encodedPassword");
        when(userPersistencePort.saveUser(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(employeeId);
            return u;
        });

        User result = userUseCase.createEmployee(validEmployee, restaurantId, ownerId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userPersistencePort).saveUser(captor.capture());
        assertEquals(UserRole.EMPLOYEE.getId(), captor.getValue().getRoleId());
        assertEquals("encodedPassword", captor.getValue().getPassword());
        verify(employeeRestaurantLinkPort).registerEmployee(restaurantId, employeeId);
        assertEquals("luis.perez@example.com", result.getEmail());
    }

    @Test
    @DisplayName("EAC-S06: Returns 404 when the restaurant does not exist")
    void shouldThrowRestaurantNotFoundExceptionWhenRestaurantDoesNotExist() {
        UUID restaurantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(restaurantOwnershipPort.getOwnerId(restaurantId))
                .thenThrow(new RestaurantNotFoundException());

        assertThrows(RestaurantNotFoundException.class,
                () -> userUseCase.createEmployee(validEmployee, restaurantId, requesterId));

        verify(userPersistencePort, never()).saveUser(any());
        verify(employeeRestaurantLinkPort, never()).registerEmployee(any(), any());
    }

    @Test
    @DisplayName("EAC-S07: Returns 403 when the caller is not the restaurant owner")
    void shouldThrowNotRestaurantOwnerExceptionWhenCallerDoesNotOwnRestaurant() {
        UUID restaurantId = UUID.randomUUID();
        UUID actualOwnerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID(); // different

        when(restaurantOwnershipPort.getOwnerId(restaurantId)).thenReturn(actualOwnerId);

        assertThrows(NotRestaurantOwnerException.class,
                () -> userUseCase.createEmployee(validEmployee, restaurantId, requesterId));

        verify(userPersistencePort, never()).saveUser(any());
        verify(employeeRestaurantLinkPort, never()).registerEmployee(any(), any());
    }

    @Test
    @DisplayName("Link registration failure triggers compensating delete and throws EmployeeLinkRegistrationException")
    void shouldDeleteUserAndThrowEmployeeLinkRegistrationExceptionOnLinkFailure() {
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(restaurantOwnershipPort.getOwnerId(restaurantId)).thenReturn(ownerId);
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(false);
        when(userPersistencePort.existsByDocumentNumber(anyString())).thenReturn(false);
        when(passwordEncoderPort.encode("plainPassword")).thenReturn("encodedPassword");
        when(userPersistencePort.saveUser(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(employeeId);
            return u;
        });
        // Simulate a non-409 failure from link registration (void method → doThrow)
        doThrow(new RuntimeException("Service unavailable"))
                .when(employeeRestaurantLinkPort).registerEmployee(any(), any());

        assertThrows(EmployeeLinkRegistrationException.class,
                () -> userUseCase.createEmployee(validEmployee, restaurantId, ownerId));

        // Compensating delete must have been called
        verify(userPersistencePort).deleteUser(employeeId);
    }

    @Test
    @DisplayName("Duplicate link (409) is rethrown as EmployeeAlreadyExistsException without compensation")
    void shouldRethrowEmployeeAlreadyExistsExceptionWithoutDeletingUserOnDuplicateLink() {
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(restaurantOwnershipPort.getOwnerId(restaurantId)).thenReturn(ownerId);
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(false);
        when(userPersistencePort.existsByDocumentNumber(anyString())).thenReturn(false);
        when(passwordEncoderPort.encode("plainPassword")).thenReturn("encodedPassword");
        when(userPersistencePort.saveUser(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(employeeId);
            return u;
        });
        // void method → doThrow
        doThrow(new EmployeeAlreadyExistsException())
                .when(employeeRestaurantLinkPort).registerEmployee(any(), any());

        assertThrows(EmployeeAlreadyExistsException.class,
                () -> userUseCase.createEmployee(validEmployee, restaurantId, ownerId));

        // No compensating delete for duplicate-link case
        verify(userPersistencePort, never()).deleteUser(any());
    }

    @Test
    @DisplayName("Rejects a duplicated email when creating an employee")
    void shouldThrowWhenEmployeeEmailAlreadyExists() {
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        when(restaurantOwnershipPort.getOwnerId(restaurantId)).thenReturn(ownerId);
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> userUseCase.createEmployee(validEmployee, restaurantId, ownerId));
        verify(userPersistencePort, never()).saveUser(any());
    }

    @Test
    @DisplayName("Rejects a duplicated document number when creating an employee")
    void shouldThrowWhenEmployeeDocumentAlreadyExists() {
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        when(restaurantOwnershipPort.getOwnerId(restaurantId)).thenReturn(ownerId);
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(false);
        when(userPersistencePort.existsByDocumentNumber(anyString())).thenReturn(true);

        assertThrows(DocumentNumberAlreadyExistsException.class,
                () -> userUseCase.createEmployee(validEmployee, restaurantId, ownerId));
        verify(userPersistencePort, never()).saveUser(any());
    }

    // ---- createClient ----

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
