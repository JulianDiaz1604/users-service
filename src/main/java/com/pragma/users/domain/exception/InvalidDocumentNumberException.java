package com.pragma.users.domain.exception;

import com.pragma.users.domain.util.DomainConstants;

public class InvalidDocumentNumberException extends DomainException {
    public InvalidDocumentNumberException() {
        super(DomainConstants.INVALID_DOCUMENT_MESSAGE);
    }
}
