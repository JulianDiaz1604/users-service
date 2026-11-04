package com.pragma.users.application.handler.impl;

import com.pragma.users.application.dto.request.LoginRequestDto;
import com.pragma.users.application.dto.response.TokenResponseDto;
import com.pragma.users.application.handler.IAuthHandler;
import com.pragma.users.domain.api.IAuthServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthHandler implements IAuthHandler {

    private static final String BEARER = "Bearer";

    private final IAuthServicePort authServicePort;

    @Override
    public TokenResponseDto login(LoginRequestDto request) {
        String token = authServicePort.login(request.getEmail(), request.getPassword());
        return new TokenResponseDto(token, BEARER);
    }
}
