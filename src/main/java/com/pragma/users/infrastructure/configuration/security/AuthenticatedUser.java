package com.pragma.users.infrastructure.configuration.security;

import java.util.UUID;

/**
 * Typed principal set in the SecurityContext by the JWT authentication filter.
 * Carries the authenticated caller's userId, role, and optional restaurantId
 * (present only for EMPLOYEE tokens; null for all other roles).
 */
public record AuthenticatedUser(UUID userId, String role, UUID restaurantId) {
}
