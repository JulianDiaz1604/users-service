package com.pragma.powerup.domain.exception;

import com.pragma.powerup.domain.util.DomainConstants;

public class UnderAgeException extends DomainException {
    public UnderAgeException() {
        super(DomainConstants.UNDERAGE_MESSAGE);
    }
}
