package com.pragma.powerup.application.handler;

import com.pragma.powerup.application.dto.request.CreateOwnerRequestDto;
import com.pragma.powerup.application.dto.response.UserResponseDto;

public interface IUserHandler {
    UserResponseDto createOwner(CreateOwnerRequestDto request);
}
