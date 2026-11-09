package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

/**
 * Thrown when the employee-restaurant link already exists in the restaurant service (HTTP 409).
 * Mapped to HTTP 409 by the existing {@code ConflictException} handler in {@code ControllerAdvisor}.
 */
// S110: intentional — mirrors the domain's ConflictException hierarchy, shared by every
// conflict exception in this package; extending ConflictException is required for the 409 mapping.
@SuppressWarnings("java:S110")
public class EmployeeAlreadyExistsException extends ConflictException {

    public EmployeeAlreadyExistsException() {
        super(DomainConstants.EMPLOYEE_ALREADY_EXISTS_MESSAGE);
    }
}
