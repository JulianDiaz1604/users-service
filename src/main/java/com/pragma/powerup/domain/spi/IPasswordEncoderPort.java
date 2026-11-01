package com.pragma.powerup.domain.spi;

public interface IPasswordEncoderPort {
    String encode(String rawPassword);
}
