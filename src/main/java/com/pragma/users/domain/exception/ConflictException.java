package com.pragma.users.domain.exception;

public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(message);
    }
}
