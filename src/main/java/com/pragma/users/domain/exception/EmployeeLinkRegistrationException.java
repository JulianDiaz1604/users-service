package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

/**
 * Thrown when the employee-restaurant link registration fails for a reason other than a
 * duplicate (e.g. restaurant service is unavailable, unexpected server error). The employee
 * account has already been rolled back (compensating delete) when this exception is raised.
 * Mapped to HTTP 502 by {@code ControllerAdvisor}.
 */
public class EmployeeLinkRegistrationException extends RuntimeException {

    public EmployeeLinkRegistrationException() {
        super(DomainConstants.EMPLOYEE_LINK_REGISTRATION_FAILED_MESSAGE);
    }
}
