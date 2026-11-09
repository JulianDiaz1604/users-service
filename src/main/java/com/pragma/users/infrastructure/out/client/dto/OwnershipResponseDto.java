package com.pragma.users.infrastructure.out.client.dto;

import java.util.UUID;

/**
 * Deserialized from {@code GET /api/v1/restaurants/{restaurantId}/owner} in food-court.
 * Shape: {@code {"restaurantId":"<uuid>","ownerId":"<uuid>"}}.
 */
public record OwnershipResponseDto(UUID restaurantId, UUID ownerId) {
}
