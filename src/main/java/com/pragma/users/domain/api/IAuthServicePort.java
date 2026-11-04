package com.pragma.users.domain.api;

/**
 * Driving port for authentication. Validates credentials and returns a signed access token.
 */
public interface IAuthServicePort {
    String login(String email, String password);
}
