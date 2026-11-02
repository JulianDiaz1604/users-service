package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

public class EmailAlreadyExistsException extends ConflictException {
    public EmailAlreadyExistsException() {
        super(DomainConstants.EMAIL_ALREADY_EXISTS_MESSAGE);
    }
}
