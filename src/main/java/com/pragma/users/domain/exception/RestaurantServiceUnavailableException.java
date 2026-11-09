package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

/**
 * Thrown when the ownership lookup against the restaurant service cannot be completed because the
 * service is unreachable (connection refused, timeout) or answers with an unexpected status.
 * Mapped to HTTP 502 by {@code ControllerAdvisor}.
 */
public class RestaurantServiceUnavailableException extends RuntimeException {

    public RestaurantServiceUnavailableException() {
        super(DomainConstants.RESTAURANT_SERVICE_UNAVAILABLE_MESSAGE);
    }
}
