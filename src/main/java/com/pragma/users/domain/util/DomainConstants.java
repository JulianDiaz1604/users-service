package com.pragma.users.domain.util;

public final class DomainConstants {

    private DomainConstants() {
    }

    public static final int MINIMUM_AGE = 18;
    public static final int PHONE_MAX_LENGTH = 13;

    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String PHONE_REGEX = "^\\+?[0-9]+$";
    public static final String DOCUMENT_REGEX = "^[0-9]+$";

    public static final String FIELD_REQUIRED_MESSAGE = "All fields are required";
    public static final String INVALID_EMAIL_MESSAGE = "The email format is invalid";
    public static final String INVALID_PHONE_MESSAGE =
            "The phone must be numeric, may start with '+', and have at most 13 characters";
    public static final String INVALID_DOCUMENT_MESSAGE = "The document number must contain digits only";
    public static final String UNDERAGE_MESSAGE = "The owner must be of legal age";
    public static final String EMAIL_ALREADY_EXISTS_MESSAGE = "A user with this email already exists";
    public static final String DOCUMENT_ALREADY_EXISTS_MESSAGE =
            "A user with this document number already exists";
    public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    // HU6 relocation — restaurant ownership + link registration
    public static final String RESTAURANT_NOT_FOUND_MESSAGE = "The restaurant does not exist";
    public static final String OWNER_NOT_FOUND_MESSAGE = "The restaurant has no registered owner";
    public static final String RESTAURANT_SERVICE_UNAVAILABLE_MESSAGE =
            "The restaurant service is unavailable; please try again later";
    public static final String NOT_RESTAURANT_OWNER_MESSAGE =
            "This action can only be performed by the owner of the restaurant";
    public static final String EMPLOYEE_ALREADY_EXISTS_MESSAGE =
            "The employee is already registered for this restaurant";
    public static final String EMPLOYEE_LINK_REGISTRATION_FAILED_MESSAGE =
            "Failed to register the employee-restaurant link; account was rolled back";
}
