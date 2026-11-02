package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

public class UnderAgeException extends DomainException {
    public UnderAgeException() {
        super(DomainConstants.UNDERAGE_MESSAGE);
    }
}
