package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

/**
 * Thrown when the specified restaurant does not exist in the restaurant service.
 * Mapped to HTTP 404 by {@code ControllerAdvisor}.
 */
public class RestaurantNotFoundException extends RuntimeException {

    public RestaurantNotFoundException() {
        super(DomainConstants.RESTAURANT_NOT_FOUND_MESSAGE);
    }
}
