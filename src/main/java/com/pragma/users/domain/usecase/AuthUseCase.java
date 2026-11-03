package com.pragma.users.domain.usecase;

import com.pragma.users.domain.api.IAuthServicePort;
import com.pragma.users.domain.exception.InvalidCredentialsException;
import com.pragma.users.domain.model.User;
import com.pragma.users.domain.model.UserRole;
import com.pragma.users.domain.spi.IPasswordEncoderPort;
import com.pragma.users.domain.spi.ITokenProviderPort;
import com.pragma.users.domain.spi.IUserPersistencePort;
import com.pragma.users.domain.validation.AuthValidator;

public class AuthUseCase implements IAuthServicePort {

    private final IUserPersistencePort userPersistencePort;
    private final IPasswordEncoderPort passwordEncoderPort;
    private final ITokenProviderPort tokenProviderPort;
    private final AuthValidator authValidator;

    public AuthUseCase(IUserPersistencePort userPersistencePort,
                       IPasswordEncoderPort passwordEncoderPort,
                       ITokenProviderPort tokenProviderPort,
                       AuthValidator authValidator) {
        this.userPersistencePort = userPersistencePort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.tokenProviderPort = tokenProviderPort;
        this.authValidator = authValidator;
    }

    @Override
    public String login(String email, String password) {
        authValidator.validateLoginRequest(email, password);

        User user = userPersistencePort.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoderPort.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return tokenProviderPort.generateToken(user.getId(), user.getEmail(),
                UserRole.fromId(user.getRoleId()));
    }
}
