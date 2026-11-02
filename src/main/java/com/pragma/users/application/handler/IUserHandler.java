package com.pragma.users.application.handler;

import com.pragma.users.application.dto.request.CreateOwnerRequestDto;
import com.pragma.users.application.dto.response.UserResponseDto;

public interface IUserHandler {
    UserResponseDto createOwner(CreateOwnerRequestDto request);

    UserResponseDto getUserById(Long id);
}
