package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

/**
 * Thrown when the employee-restaurant link already exists in the restaurant service (HTTP 409).
 * Mapped to HTTP 409 by the existing {@code ConflictException} handler in {@code ControllerAdvisor}.
 */
public class EmployeeAlreadyExistsException extends ConflictException {

    public EmployeeAlreadyExistsException() {
        super(DomainConstants.EMPLOYEE_ALREADY_EXISTS_MESSAGE);
    }
}
