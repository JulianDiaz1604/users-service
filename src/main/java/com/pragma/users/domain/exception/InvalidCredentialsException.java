package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

/**
 * Raised when the email does not exist or the password does not match.
 * Mapped to HTTP 401 and deliberately generic so it never reveals which part failed.
 */
public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super(DomainConstants.INVALID_CREDENTIALS_MESSAGE);
    }
}
