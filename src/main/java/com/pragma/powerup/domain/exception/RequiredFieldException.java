package com.pragma.powerup.domain.exception;

import com.pragma.powerup.domain.util.DomainConstants;

public class RequiredFieldException extends DomainException {
    public RequiredFieldException() {
        super(DomainConstants.FIELD_REQUIRED_MESSAGE);
    }
}
