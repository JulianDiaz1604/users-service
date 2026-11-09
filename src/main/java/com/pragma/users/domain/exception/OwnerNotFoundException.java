package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

/**
 * Thrown when the restaurant exists but the restaurant service does not report an owner for it
 * (missing {@code ownerId} in the ownership response). Mapped to HTTP 404 by {@code ControllerAdvisor}.
 */
public class OwnerNotFoundException extends RuntimeException {

    public OwnerNotFoundException() {
        super(DomainConstants.OWNER_NOT_FOUND_MESSAGE);
    }
}
