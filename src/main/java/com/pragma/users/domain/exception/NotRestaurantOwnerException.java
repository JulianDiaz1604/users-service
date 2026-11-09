package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

/**
 * Thrown when the authenticated caller is not the owner of the specified restaurant.
 * Mapped to HTTP 403 by {@code ControllerAdvisor}.
 */
public class NotRestaurantOwnerException extends RuntimeException {

    public NotRestaurantOwnerException() {
        super(DomainConstants.NOT_RESTAURANT_OWNER_MESSAGE);
    }
}
