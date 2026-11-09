package com.pragma.users.application.handler.impl;

import com.pragma.users.application.dto.request.CreateClientRequestDto;
import com.pragma.users.application.dto.request.CreateEmployeeRequestDto;
import com.pragma.users.application.dto.request.CreateOwnerRequestDto;
import com.pragma.users.application.dto.response.UserResponseDto;
import com.pragma.users.application.handler.IUserHandler;
import com.pragma.users.application.mapper.IUserRequestMapper;
import com.pragma.users.application.mapper.IUserResponseMapper;
import com.pragma.users.domain.api.IUserServicePort;
import com.pragma.users.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserHandler implements IUserHandler {

    private final IUserServicePort userServicePort;
    private final IUserRequestMapper userRequestMapper;
    private final IUserResponseMapper userResponseMapper;

    @Override
    public UserResponseDto createOwner(CreateOwnerRequestDto request) {
        User createdOwner = userServicePort.createOwner(userRequestMapper.toUser(request));
        return userResponseMapper.toResponse(createdOwner);
    }

    @Override
    public UserResponseDto createEmployee(CreateEmployeeRequestDto request, UUID requesterOwnerId) {
        User employee = userRequestMapper.toUser(request);
        User createdEmployee = userServicePort.createEmployee(employee, request.getRestaurantId(), requesterOwnerId);
        return userResponseMapper.toResponse(createdEmployee);
    }

    @Override
    public UserResponseDto createClient(CreateClientRequestDto request) {
        User createdClient = userServicePort.createClient(userRequestMapper.toUser(request));
        return userResponseMapper.toResponse(createdClient);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(UUID id) {
        return userResponseMapper.toResponse(userServicePort.getUserById(id));
    }
}
