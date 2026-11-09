package com.pragma.users.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeRequestDto {
    private String name;
    private String lastName;
    private String documentNumber;
    private String phone;
    private String email;
    private String password;
    private UUID restaurantId;
}
