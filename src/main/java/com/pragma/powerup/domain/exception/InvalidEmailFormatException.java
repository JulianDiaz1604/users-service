package com.pragma.powerup.domain.exception;

import com.pragma.powerup.domain.util.DomainConstants;

public class InvalidEmailFormatException extends DomainException {
    public InvalidEmailFormatException() {
        super(DomainConstants.INVALID_EMAIL_MESSAGE);
    }
}
