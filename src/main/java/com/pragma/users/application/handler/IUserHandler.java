package com.pragma.users.application.handler;

import com.pragma.users.application.dto.request.CreateEmployeeRequestDto;
import com.pragma.users.application.dto.request.CreateOwnerRequestDto;
import com.pragma.users.application.dto.response.UserResponseDto;

import java.util.UUID;

public interface IUserHandler {
    UserResponseDto createOwner(CreateOwnerRequestDto request);

    UserResponseDto createEmployee(CreateEmployeeRequestDto request);

    UserResponseDto getUserById(UUID id);
}
