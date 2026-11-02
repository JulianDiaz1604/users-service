package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

public class RequiredFieldException extends DomainException {
    public RequiredFieldException() {
        super(DomainConstants.FIELD_REQUIRED_MESSAGE);
    }
}
