package com.pragma.users.domain.spi;

import com.pragma.users.domain.model.UserRole;

import java.util.UUID;

/**
 * Driven port for issuing authentication tokens. The domain only knows how to ask for a token;
 * the signing technology (JWT, etc.) lives in an infrastructure adapter.
 */
public interface ITokenProviderPort {
    String generateToken(UUID userId, String email, UserRole role);
}
