package com.pragma.powerup.domain.exception;

import com.pragma.powerup.domain.util.DomainConstants;

public class InvalidDocumentNumberException extends DomainException {
    public InvalidDocumentNumberException() {
        super(DomainConstants.INVALID_DOCUMENT_MESSAGE);
    }
}
