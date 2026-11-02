package com.pragma.users.infrastructure.input.rest;

import com.pragma.users.application.dto.request.CreateOwnerRequestDto;
import com.pragma.users.application.dto.response.UserResponseDto;
import com.pragma.users.application.handler.IUserHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserRestController {

    private final IUserHandler userHandler;

    @Operation(summary = "Create an owner account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Owner account created"),
            @ApiResponse(responseCode = "400", description = "Invalid owner data"),
            @ApiResponse(responseCode = "409", description = "Email or document number already registered")
    })
    @PostMapping("/owner")
    public ResponseEntity<UserResponseDto> createOwner(@RequestBody CreateOwnerRequestDto request) {
        return new ResponseEntity<>(userHandler.createOwner(request), HttpStatus.CREATED);
    }
}
