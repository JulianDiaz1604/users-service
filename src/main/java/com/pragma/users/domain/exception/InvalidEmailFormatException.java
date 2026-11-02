package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

public class InvalidEmailFormatException extends DomainException {
    public InvalidEmailFormatException() {
        super(DomainConstants.INVALID_EMAIL_MESSAGE);
    }
}
