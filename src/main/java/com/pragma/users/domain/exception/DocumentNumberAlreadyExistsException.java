package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

public class DocumentNumberAlreadyExistsException extends ConflictException {
    public DocumentNumberAlreadyExistsException() {
        super(DomainConstants.DOCUMENT_ALREADY_EXISTS_MESSAGE);
    }
}
