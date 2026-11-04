package com.pragma.users.infrastructure.input.rest;

import com.pragma.users.application.dto.request.CreateClientRequestDto;
import com.pragma.users.application.dto.request.CreateEmployeeRequestDto;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

    @Operation(summary = "Create an employee account (owner only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Employee account created"),
            @ApiResponse(responseCode = "400", description = "Invalid employee data"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "403", description = "Caller is not an owner"),
            @ApiResponse(responseCode = "409", description = "Email or document number already registered")
    })
    @PostMapping("/employee")
    public ResponseEntity<UserResponseDto> createEmployee(@RequestBody CreateEmployeeRequestDto request) {
        return new ResponseEntity<>(userHandler.createEmployee(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Create a client account (public self-registration)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Client account created"),
            @ApiResponse(responseCode = "400", description = "Invalid client data"),
            @ApiResponse(responseCode = "409", description = "Email or document number already registered")
    })
    @PostMapping("/client")
    public ResponseEntity<UserResponseDto> createClient(@RequestBody CreateClientRequestDto request) {
        return new ResponseEntity<>(userHandler.createClient(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Get a user by id (used by other services to validate roles)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userHandler.getUserById(id));
    }
}
