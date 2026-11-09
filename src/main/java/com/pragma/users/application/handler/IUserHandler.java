package com.pragma.users.application.handler;

import com.pragma.users.application.dto.request.CreateClientRequestDto;
import com.pragma.users.application.dto.request.CreateEmployeeRequestDto;
import com.pragma.users.application.dto.request.CreateOwnerRequestDto;
import com.pragma.users.application.dto.response.UserResponseDto;

import java.util.UUID;

public interface IUserHandler {
    UserResponseDto createOwner(CreateOwnerRequestDto request);

    /**
     * Creates an employee account via the orchestrated use case.
     *
     * @param request          the employee request data (must include restaurantId)
     * @param requesterOwnerId the authenticated caller's userId (from the JWT subject)
     * @return the created employee response (password excluded)
     */
    UserResponseDto createEmployee(CreateEmployeeRequestDto request, UUID requesterOwnerId);

    UserResponseDto createClient(CreateClientRequestDto request);

    UserResponseDto getUserById(UUID id);
}
