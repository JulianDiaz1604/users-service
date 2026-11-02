package com.pragma.users.domain.spi;

public interface IPasswordEncoderPort {
    String encode(String rawPassword);
}
