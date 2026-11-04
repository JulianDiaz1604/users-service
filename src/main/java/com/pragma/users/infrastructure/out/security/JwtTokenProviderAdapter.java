package com.pragma.users.infrastructure.out.security;

import com.pragma.users.domain.model.UserRole;
import com.pragma.users.domain.spi.ITokenProviderPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT adapter: signs tokens on login (driven port) and parses/validates them for the request filter.
 * HS256 symmetric signing with a secret shared across the microservices so every service can verify.
 */
public class JwtTokenProviderAdapter implements ITokenProviderPort {

    public static final String ROLE_CLAIM = "role";
    public static final String EMAIL_CLAIM = "email";

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtTokenProviderAdapter(String secret, long expirationMillis) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    @Override
    public String generateToken(UUID userId, String email, UserRole role) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(userId.toString())
                .claim(ROLE_CLAIM, role.name())
                .claim(EMAIL_CLAIM, email)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(signingKey)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractRole(String token) {
        return parseClaims(token).get(ROLE_CLAIM, String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
