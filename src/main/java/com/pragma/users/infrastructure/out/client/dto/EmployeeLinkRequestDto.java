package com.pragma.users.infrastructure.out.client.dto;

import java.util.UUID;

/**
 * Request body sent to {@code POST /api/v1/restaurants/{restaurantId}/employees} in food-court.
 * Shape: {@code {"employeeId":"<uuid>"}}.
 */
public record EmployeeLinkRequestDto(UUID employeeId) {
}
