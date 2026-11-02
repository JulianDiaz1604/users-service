package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

public class InvalidPhoneFormatException extends DomainException {
    public InvalidPhoneFormatException() {
        super(DomainConstants.INVALID_PHONE_MESSAGE);
    }
}
