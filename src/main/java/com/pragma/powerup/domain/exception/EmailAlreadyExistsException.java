package com.pragma.powerup.domain.exception;

import com.pragma.powerup.domain.util.DomainConstants;

public class EmailAlreadyExistsException extends ConflictException {
    public EmailAlreadyExistsException() {
        super(DomainConstants.EMAIL_ALREADY_EXISTS_MESSAGE);
    }
}
