package com.pragma.users.infrastructure.configuration;

import com.pragma.users.domain.api.IAuthServicePort;
import com.pragma.users.domain.api.IUserServicePort;
import com.pragma.users.domain.spi.IPasswordEncoderPort;
import com.pragma.users.domain.spi.IUserPersistencePort;
import com.pragma.users.domain.usecase.AuthUseCase;
import com.pragma.users.domain.usecase.UserUseCase;
import com.pragma.users.domain.validation.AuthValidator;
import com.pragma.users.domain.validation.UserValidator;
import com.pragma.users.infrastructure.out.jpa.adapter.UserJpaAdapter;
import com.pragma.users.infrastructure.out.jpa.mapper.IUserEntityMapper;
import com.pragma.users.infrastructure.out.jpa.repository.IUserRepository;
import com.pragma.users.infrastructure.out.security.BcryptPasswordEncoderAdapter;
import com.pragma.users.infrastructure.out.security.JwtTokenProviderAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class BeanConfiguration {

    private final IUserRepository userRepository;
    private final IUserEntityMapper userEntityMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public IPasswordEncoderPort passwordEncoderPort() {
        return new BcryptPasswordEncoderAdapter(passwordEncoder());
    }

    @Bean
    public IUserPersistencePort userPersistencePort() {
        return new UserJpaAdapter(userRepository, userEntityMapper);
    }

    @Bean
    public UserValidator userValidator() {
        return new UserValidator();
    }

    @Bean
    public IUserServicePort userServicePort() {
        return new UserUseCase(userPersistencePort(), passwordEncoderPort(), userValidator());
    }

    @Bean
    public JwtTokenProviderAdapter tokenProviderAdapter(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long jwtExpiration) {
        return new JwtTokenProviderAdapter(jwtSecret, jwtExpiration);
    }

    @Bean
    public AuthValidator authValidator() {
        return new AuthValidator();
    }

    @Bean
    public IAuthServicePort authServicePort(JwtTokenProviderAdapter tokenProviderAdapter) {
        return new AuthUseCase(userPersistencePort(), passwordEncoderPort(),
                tokenProviderAdapter, authValidator());
    }
}
