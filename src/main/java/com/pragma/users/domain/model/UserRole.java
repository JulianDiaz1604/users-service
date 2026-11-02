package com.pragma.users.domain.model;

public enum UserRole {
    ADMINISTRATOR(1L),
    OWNER(2L),
    EMPLOYEE(3L),
    CLIENT(4L);

    private final Long id;

    UserRole(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
