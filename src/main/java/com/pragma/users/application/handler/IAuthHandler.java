package com.pragma.users.application.handler;

import com.pragma.users.application.dto.request.LoginRequestDto;
import com.pragma.users.application.dto.response.TokenResponseDto;

public interface IAuthHandler {
    TokenResponseDto login(LoginRequestDto request);
}
