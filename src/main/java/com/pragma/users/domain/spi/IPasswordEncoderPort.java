package com.pragma.users.domain.spi;

public interface IPasswordEncoderPort {
    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
