package com.pragma.powerup.domain.exception;

import com.pragma.powerup.domain.util.DomainConstants;

public class InvalidPhoneFormatException extends DomainException {
    public InvalidPhoneFormatException() {
        super(DomainConstants.INVALID_PHONE_MESSAGE);
    }
}
