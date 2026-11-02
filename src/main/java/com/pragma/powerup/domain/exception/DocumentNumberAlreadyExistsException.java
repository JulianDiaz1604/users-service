package com.pragma.powerup.domain.exception;

import com.pragma.powerup.domain.util.DomainConstants;

public class DocumentNumberAlreadyExistsException extends ConflictException {
    public DocumentNumberAlreadyExistsException() {
        super(DomainConstants.DOCUMENT_ALREADY_EXISTS_MESSAGE);
    }
}
