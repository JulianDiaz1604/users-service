package com.pragma.users.domain.validation;

import com.pragma.users.domain.exception.RequiredFieldException;

/**
 * Validates the shape of a login request. Credential correctness (user lookup + password match)
 * is a use-case concern; this validator only guarantees the mandatory fields are present.
 */
public class AuthValidator {

    public void validateLoginRequest(String email, String password) {
        if (isBlank(email) || isBlank(password)) {
            throw new RequiredFieldException();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
